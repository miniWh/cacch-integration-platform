package com.cacch.integration.service.mdm.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.constant.ihr.IhrStaffConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpOrgAssignConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpEventConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.config.mdm.MdmEmpProperties;
import com.cacch.integration.common.enums.mdm.MdmEmpIngestResultEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.common.utils.mdm.MdmEmpUtils;
import com.cacch.integration.entity.mdm.MdEmpDO;
import com.cacch.integration.entity.mdm.MdEmpOrgAssignDO;
import com.cacch.integration.entity.mdm.OdsIhrEmpDO;
import com.cacch.integration.entity.mdm.StgIhrEmpDO;
import com.cacch.integration.entity.mdm.SysEventDO;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicInfo;
import com.cacch.integration.mapper.mdm.MdEmpMapper;
import com.cacch.integration.mapper.mdm.MdEmpOrgAssignMapper;
import com.cacch.integration.mapper.mdm.OdsIhrEmpMapper;
import com.cacch.integration.mapper.mdm.StgIhrEmpMapper;
import com.cacch.integration.mapper.mdm.SysEventMapper;
import com.cacch.integration.service.mdm.api.IMdmEmpIngestService;
import com.cacch.integration.service.mdm.convert.MdmEmpConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 人员主数据（MDM）单员工落库 Service 实现
 *
 * <p>单员工事务粒度（纯 DB 操作，无 HTTP / Redis），由 Manager 编排调用。
 * 链路：stg 贴源 → rowHash 幂等短路 → ods upsert → 证件号质检 →
 * md_emp 主档 upsert（idHash 识别自然人）→ 任职拉链 → sys_event。</p>
 *
 * <p>主键策略：手写 SQL（stg / ods / sys_event）由本类调用
 * {@link IdWorker#getId()} 预生成雪花主键；走 BaseMapper 的
 * md_emp / md_emp_org_assign 由 ASSIGN_ID 自动填充（mdCode 需显式预生成）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmEmpIngestServiceImpl implements IMdmEmpIngestService {

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;

    /**
     * rowHash / payload 构造用字段分隔符
     */
    private static final String FIELD_SEP = "|";

    /**
     * 静态 ObjectMapper —— 仅用于事件 payload 序列化
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final StgIhrEmpMapper stgIhrEmpMapper;
    private final OdsIhrEmpMapper odsIhrEmpMapper;
    private final MdEmpMapper mdEmpMapper;
    private final MdEmpOrgAssignMapper mdEmpOrgAssignMapper;
    private final SysEventMapper sysEventMapper;
    private final MdmEmpProperties mdmEmpProperties;
    private final MdmEmpConverter mdmEmpConverter;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public MdmEmpIngestResultEnum ingestOne(IhrStaffBasicInfo basic, String rawJson, String etlBatchNo) {
        String staffId = basic.getStaffId();

        // 1. stg 贴源（同批次重复报文幂等吞掉）
        StgIhrEmpDO stg = new StgIhrEmpDO();
        stg.setId(IdWorker.getId());
        stg.setEtlBatchNo(etlBatchNo);
        stg.setIhrStaffId(staffId);
        stg.setRawJson(rawJson);
        stg.setHttpStatus(HttpStatus.OK.value());
        stgIhrEmpMapper.insertIgnore(stg);

        // 2. rowHash 比对 —— 未变化则幂等短路
        String rowHash = buildRowHash(basic);
        OdsIhrEmpDO existing = odsIhrEmpMapper.selectOne(new LambdaQueryWrapper<OdsIhrEmpDO>()
                .eq(OdsIhrEmpDO::getIhrStaffId, staffId)
                .last("LIMIT 1"));
        if (existing != null && rowHash.equals(existing.getRowHash())) {
            log.info("【{}】ods 数据无变化, 幂等跳过后续处理, ihrStaffId={}, batchNo={}", BIZ, staffId, etlBatchNo);
            return MdmEmpIngestResultEnum.SKIPPED_UNCHANGED;
        }

        // 3. 数据质检：证件号缺失（致命）；离职无离职日期（置 dq_flag 继续）
        boolean idCardMissing = !StringUtils.hasText(basic.getIdCardNo());
        boolean quitWithoutLeave = IhrStaffConstants.STAFF_STATUS_QUIT.equals(basic.getStaffStatus())
                && !StringUtils.hasText(basic.getLeaveDate());
        if (idCardMissing) {
            log.warn("【{}】证件号缺失, 无法建立自然人主档, ihrStaffId={}, staffNo={}",
                    BIZ, staffId, basic.getStaffNo());
            log.info("【{}】质检致命项终止本员工主档与事件处理, ihrStaffId={}, batchNo={}", BIZ, staffId, etlBatchNo);
        } else if (quitWithoutLeave) {
            log.warn("【{}】离职状态但离职日期缺失, 置数据质量标记, ihrStaffId={}, staffNo={}",
                    BIZ, staffId, basic.getStaffNo());
            log.info("【{}】质检非致命项, 继续落库, ihrStaffId={}, batchNo={}", BIZ, staffId, etlBatchNo);
        }

        // 4. ods 规范化 upsert（verNo 递增）
        OdsIhrEmpDO ods = mdmEmpConverter.toOds(basic, IdWorker.getId(), rowHash,
                existing == null || existing.getVerNo() == null ? 1 : existing.getVerNo() + 1,
                idCardMissing || quitWithoutLeave ? 1 : 0);
        odsIhrEmpMapper.upsert(ods);

        if (idCardMissing) {
            return MdmEmpIngestResultEnum.SKIPPED_MISSING_IDCARD;
        }

        // 5. md_emp 主档 upsert（idHash 识别自然人，工号变更不断链）
        String idHash = MdmEmpUtils.sha256Hex(mdmEmpProperties.getIdHashSalt() + basic.getIdCardNo());
        MdEmpDO mdEmp = mdEmpMapper.selectOne(new LambdaQueryWrapper<MdEmpDO>()
                .eq(MdEmpDO::getIdHash, idHash)
                .last("LIMIT 1"));
        boolean mdExisted = mdEmp != null;
        LocalDate enrollInDate = MdmEmpUtils.parseDate(basic.getEnrollInDate(), staffId, "enrollInDate");
        if (mdEmp == null) {
            mdEmp = new MdEmpDO();
            mdEmp.setMdCode(IdWorker.getId());
            mdEmp.setIdHash(idHash);
            mdEmp.setFirstHireDate(enrollInDate);
            mdEmp.setSrcSysCode(MdmEmpCommonConstants.SRC_SYS_CODE_IHR);
        }
        mdmEmpConverter.updateBasicInfo(basic, mdEmp);
        if (mdExisted) {
            mdEmpMapper.updateById(mdEmp);
        } else {
            mdEmpMapper.insert(mdEmp);
        }

        // 6. 任职拉链判定 + 事件产生
        String eventType = resolveAssignAndEvent(mdEmp, basic, enrollInDate, mdExisted, etlBatchNo, rowHash);

        log.info("【{}】单员工落库完成, ihrStaffId={}, mdCode={}, eventType={}, batchNo={}",
                BIZ, staffId, mdEmp.getMdCode(), eventType, etlBatchNo);
        return MdmEmpIngestResultEnum.INGESTED;
    }

    /**
     * 任职拉链判定（关旧 / 开新 / 更新）并写入变更事件
     *
     * <p>判定规则（按当前有效行 validTo IS NULL）：
     * <ul>
     *     <li>无有效行 → 新开任职；主档已存在为 REHIRE，否则 HIRE（HIGH）</li>
     *     <li>离职 → 关链（validTo=离职日期，状态 ENDED），LEAVE（HIGH）</li>
     *     <li>部门或职位变更 → 旧行关链（validTo=入职日期）+ 新开行，TRANSFER（NORMAL）</li>
     *     <li>其余属性变更 → 更新当前行组织/职位名，UPDATE（NORMAL）</li>
     * </ul></p>
     *
     * @param mdEmp        主档 DO（已落库，含 mdCode）
     * @param basic        iHR 员工基本信息
     * @param enrollInDate 解析后的入职日期（可为 null）
     * @param mdExisted    主档是否已存在（区分 HIRE / REHIRE）
     * @param etlBatchNo   ETL 批次号
     * @param rowHash      行哈希（事件幂等键组成部分）
     * @return 事件类型（HIRE / REHIRE / LEAVE / TRANSFER / UPDATE）
     */
    private String resolveAssignAndEvent(MdEmpDO mdEmp, IhrStaffBasicInfo basic, LocalDate enrollInDate,
                                         boolean mdExisted, String etlBatchNo, String rowHash) {
        String staffId = basic.getStaffId();
        Long mdCode = mdEmp.getMdCode();
        String orgCode = MdmEmpUtils.longToStringSafe(basic.getDepartmentId());
        String orgName = basic.getDepartmentName();
        String positionCode = basic.getPositionId();
        String positionName = basic.getPositionName();
        LocalDate probationEndDate = MdmEmpUtils.parseDate(basic.getProbationEndDate(), staffId, "probationEndDate");
        LocalDate leaveDate = MdmEmpUtils.parseDate(basic.getLeaveDate(), staffId, "leaveDate");
        boolean quit = IhrStaffConstants.STAFF_STATUS_QUIT.equals(basic.getStaffStatus());

        MdEmpOrgAssignDO active = mdEmpOrgAssignMapper.selectOne(new LambdaQueryWrapper<MdEmpOrgAssignDO>()
                .eq(MdEmpOrgAssignDO::getMdCode, mdCode)
                .isNull(MdEmpOrgAssignDO::getValidTo)
                .last("LIMIT 1"));

        String eventType;
        if (active == null) {
            // 新开任职（首次建档或再入职）
            insertAssign(mdCode, basic, enrollInDate, probationEndDate, leaveDate, staffId);
            eventType = mdExisted ? MdmEmpEventConstants.EVENT_TYPE_REHIRE : MdmEmpEventConstants.EVENT_TYPE_HIRE;
            insertEvent(mdEmp, basic, eventType, MdmEmpEventConstants.CHANGE_LEVEL_HIGH,
                    orgCode, orgName, positionCode, positionName, enrollInDate, null, etlBatchNo, rowHash);
        } else if (quit) {
            // 离职关链；离职日期缺失时保留有效行（validTo 语义为 NULL=当前有效）
            if (leaveDate != null) {
                active.setValidTo(leaveDate);
                active.setAssignStatus(MdmEmpOrgAssignConstants.ASSIGN_STATUS_ENDED);
                mdEmpOrgAssignMapper.updateById(active);
            } else {
                log.info("【{}】离职但无离职日期, 任职行暂不关链, ihrStaffId={}, mdCode={}", BIZ, staffId, mdCode);
            }
            eventType = MdmEmpEventConstants.EVENT_TYPE_LEAVE;
            insertEvent(mdEmp, basic, eventType, MdmEmpEventConstants.CHANGE_LEVEL_HIGH,
                    active.getOrgCode(), active.getOrgName(), active.getPositionCode(), active.getPositionName(),
                    active.getValidFrom(), leaveDate, etlBatchNo, rowHash);
        } else if (!Objects.equals(active.getOrgCode(), orgCode)
                || !Objects.equals(active.getPositionCode(), positionCode)) {
            // 调岗：旧行关链（validTo=入职日期）+ 新开行
            active.setValidTo(enrollInDate);
            active.setAssignStatus(MdmEmpOrgAssignConstants.ASSIGN_STATUS_ENDED);
            mdEmpOrgAssignMapper.updateById(active);
            insertAssign(mdCode, basic, enrollInDate, probationEndDate, leaveDate, staffId);
            eventType = MdmEmpEventConstants.EVENT_TYPE_TRANSFER;
            insertEvent(mdEmp, basic, eventType, MdmEmpEventConstants.CHANGE_LEVEL_NORMAL,
                    orgCode, orgName, positionCode, positionName, enrollInDate, null, etlBatchNo, rowHash);
        } else {
            // 属性更新：仅刷新当前行的组织/职位名称
            active.setOrgName(orgName);
            active.setPositionName(positionName);
            active.setAssignStatus(deriveAssignStatus(basic.getStaffStatus(), enrollInDate, probationEndDate, leaveDate));
            mdEmpOrgAssignMapper.updateById(active);
            eventType = MdmEmpEventConstants.EVENT_TYPE_UPDATE;
            insertEvent(mdEmp, basic, eventType, MdmEmpEventConstants.CHANGE_LEVEL_NORMAL,
                    orgCode, orgName, positionCode, positionName, active.getValidFrom(), null, etlBatchNo, rowHash);
        }
        return eventType;
    }

    /**
     * 新开任职行（主任职，validTo 为 NULL 表示当前有效）
     *
     * @param mdCode           人员主数据编码
     * @param basic            iHR 员工基本信息（Converter 复用 departmentId / departmentName / positionId / positionName）
     * @param enrollInDate     解析后的入职日期
     * @param probationEndDate 解析后的试用结束日期
     * @param leaveDate        解析后的离职日期
     * @param staffId          员工 ID（日志定位用）
     */
    private void insertAssign(Long mdCode, IhrStaffBasicInfo basic, LocalDate enrollInDate,
                              LocalDate probationEndDate, LocalDate leaveDate, String staffId) {
        MdEmpOrgAssignDO assign = mdmEmpConverter.toAssign(basic, mdCode,
                deriveAssignStatus(basic.getStaffStatus(), enrollInDate, probationEndDate, leaveDate),
                MdmEmpOrgAssignConstants.PRINCIPAL_FLAG_YES, enrollInDate);
        mdEmpOrgAssignMapper.insert(assign);
    }

    /**
     * 写入变更事件（幂等键冲突时静默吞掉）
     *
     * <p>eventId = SHA-256(mdCode:eventType:rowHash)：内容不变的事件重复产生时
     * 幂等去重，真实内容变化（rowHash 变化）则生成新事件。</p>
     *
     * @param mdEmp        主档 DO（含 mdCode / empNo）
     * @param basic        iHR 员工基本信息
     * @param eventType    事件类型
     * @param changeLevel  变更级别
     * @param orgCode      事件发生时组织编码
     * @param orgName      组织名称
     * @param positionCode 职位编码
     * @param positionName 职位名称
     * @param validFrom    任职生效起始日
     * @param validTo      任职生效截止日（可为 null）
     * @param etlBatchNo   ETL 批次号
     * @param rowHash      行哈希（幂等键组成部分）
     */
    private void insertEvent(MdEmpDO mdEmp, IhrStaffBasicInfo basic, String eventType, String changeLevel,
                             String orgCode, String orgName, String positionCode, String positionName,
                             LocalDate validFrom, LocalDate validTo, String etlBatchNo, String rowHash) {
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("mdCode", mdEmp.getMdCode());
        payloadMap.put("empNo", mdEmp.getEmpNo());
        payloadMap.put("empName", mdEmp.getEmpName());
        payloadMap.put("staffStatus", basic.getStaffStatus());
        payloadMap.put("orgCode", orgCode);
        payloadMap.put("orgName", orgName);
        payloadMap.put("positionCode", positionCode);
        payloadMap.put("positionName", positionName);
        payloadMap.put("validFrom", validFrom == null ? null : validFrom.toString());
        payloadMap.put("validTo", validTo == null ? null : validTo.toString());
        payloadMap.put("etlBatchNo", etlBatchNo);

        String payload;
        try {
            payload = MAPPER.writeValueAsString(payloadMap);
        } catch (Exception e) {
            log.error("【{}】事件 payload 序列化失败, mdCode={}, eventType={}", BIZ, mdEmp.getMdCode(), eventType, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "事件 payload 序列化失败");
        }

        String eventId = MdmEmpUtils.sha256Hex(mdEmp.getMdCode() + ":" + eventType + ":" + rowHash);
        SysEventDO event = new SysEventDO();
        event.setId(IdWorker.getId());
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setChangeLevel(changeLevel);
        event.setMdCode(mdEmp.getMdCode());
        event.setEmpNo(mdEmp.getEmpNo());
        event.setPayload(payload);
        event.setEtlBatchNo(etlBatchNo);
        event.setStatus(MdmEmpEventConstants.EVENT_STATUS_PENDING);
        int rows = sysEventMapper.insertIgnore(event);
        if (rows == 0) {
            log.info("【{}】事件已存在, 幂等跳过, mdCode={}, eventType={}, eventId={}",
                    BIZ, mdEmp.getMdCode(), eventType, eventId);
        }
    }

    /**
     * 构造行哈希 —— SHA-256(工号|姓名|手机号|工作邮箱|性别|生日|学历|部门|职位|状态|入职|离职)
     *
     * <p>覆盖「人属性 + 任职 + 状态」的关键字段集合，任一变化即视为行变更；
     * null 统一按空串参与拼接，保证哈希稳定。</p>
     *
     * @param basic iHR 员工基本信息
     * @return 64 位小写 hex 哈希
     */
    private String buildRowHash(IhrStaffBasicInfo basic) {
        String joined = String.join(FIELD_SEP,
                MdmEmpUtils.nullToEmpty(basic.getStaffNo()),
                MdmEmpUtils.nullToEmpty(basic.getStaffName()),
                MdmEmpUtils.nullToEmpty(basic.getMobileNo()),
                MdmEmpUtils.nullToEmpty(basic.getWorkEmail()),
                MdmEmpUtils.nullToEmpty(basic.getSex()),
                MdmEmpUtils.nullToEmpty(basic.getBirthday()),
                MdmEmpUtils.nullToEmpty(basic.getHighestEducation()),
                basic.getDepartmentId() == null ? "" : String.valueOf(basic.getDepartmentId()),
                MdmEmpUtils.nullToEmpty(basic.getPositionId()),
                MdmEmpUtils.nullToEmpty(basic.getStaffStatus()),
                MdmEmpUtils.nullToEmpty(basic.getEnrollInDate()),
                MdmEmpUtils.nullToEmpty(basic.getLeaveDate()));
        return MdmEmpUtils.sha256Hex(joined);
    }

    /**
     * 派生任职状态（MDM 域专有业务规则，保留 Service 内部）
     *
     * <p>规则（按当天判断）：已过离职日期 → ENDED；入职日期晚于当天 →
     * PENDING_ONBOARD；当天不晚于试用结束日期 → PROBATION；否则 ACTIVE。</p>
     *
     * @param staffStatus      iHR 员工状态（IN_SERVICE / QUIT）
     * @param enrollInDate     入职日期（可为 null）
     * @param probationEndDate 试用结束日期（可为 null）
     * @param leaveDate        离职日期（可为 null）
     * @return 任职状态常量（见 {@link MdmEmpOrgAssignConstants}）
     */
    private String deriveAssignStatus(String staffStatus, LocalDate enrollInDate,
                                      LocalDate probationEndDate, LocalDate leaveDate) {
        LocalDate today = LocalDate.now();
        if (IhrStaffConstants.STAFF_STATUS_QUIT.equals(staffStatus)
                && leaveDate != null && leaveDate.isBefore(today)) {
            return MdmEmpOrgAssignConstants.ASSIGN_STATUS_ENDED;
        }
        if (enrollInDate != null && enrollInDate.isAfter(today)) {
            return MdmEmpOrgAssignConstants.ASSIGN_STATUS_PENDING_ONBOARD;
        }
        if (probationEndDate != null && !today.isAfter(probationEndDate)) {
            return MdmEmpOrgAssignConstants.ASSIGN_STATUS_PROBATION;
        }
        return MdmEmpOrgAssignConstants.ASSIGN_STATUS_ACTIVE;
    }
}
