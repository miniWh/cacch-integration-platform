package com.cacch.integration.service.mdm.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.constant.ihr.IhrStaffConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpOrgAssignConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpEventConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.enums.mdm.MdmEmpIngestResultEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.common.utils.mdm.MdmEmpUtils;
import com.cacch.integration.entity.mdm.MdmEmpDO;
import com.cacch.integration.entity.mdm.MdmEmpEventDO;
import com.cacch.integration.entity.mdm.MdmEmpOrgAssignDO;
import com.cacch.integration.entity.mdm.MdmEmpSrcRefDO;
import com.cacch.integration.entity.mdm.OdsIhrEmpDO;
import com.cacch.integration.entity.mdm.StgIhrEmpDO;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicInfo;
import com.cacch.integration.mapper.mdm.MdmEmpEventMapper;
import com.cacch.integration.mapper.mdm.MdmEmpMapper;
import com.cacch.integration.mapper.mdm.MdmEmpOrgAssignMapper;
import com.cacch.integration.mapper.mdm.MdmEmpSrcRefMapper;
import com.cacch.integration.mapper.mdm.OdsIhrEmpMapper;
import com.cacch.integration.mapper.mdm.StgIhrEmpMapper;
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
 * src_ref 匹配主档（src_sys_code + src_entity_id）→ mdm_emp upsert →
 * src_ref insertIgnore → 任职拉链 → mdm_emp_event。</p>
 *
 * <p>主键策略：手写 SQL（stg / ods / mdm_emp_event / src_ref）由本类调用
 * {@link IdWorker#getId()} 预生成雪花主键；走 BaseMapper 的
 * mdm_emp / mdm_emp_org_assign 由 ASSIGN_ID 自动填充（mdCode 需显式预生成）。</p>
 *
 * <p>主档匹配（替代原 idHash 方案）：ingest 入口先查
 * {@link MdmEmpSrcRefDO}（src_sys_code=IHR + src_entity_type=STAFF +
 * src_entity_id=staffId），拿到 md_code 再查 mdm_emp；无 src_ref →
 * 无主档 → INSERT 新主档 + src_ref insertIgnore。自然人身份识别由
 * 多字段匹配（证件号 + 姓名 + 手机号 + 工号等）在后续人工核对或专用匹配流程中完成。</p>
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
     * src_ref 匹配：来源系统编码与实体类型（IHR 员工域）
     */
    private static final String SRC_ENTITY_TYPE_STAFF = "STAFF";

    /**
     * 静态 ObjectMapper —— 仅用于事件 payload 序列化
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final StgIhrEmpMapper stgIhrEmpMapper;
    private final OdsIhrEmpMapper odsIhrEmpMapper;
    private final MdmEmpMapper mdEmpMapper;
    private final MdmEmpOrgAssignMapper mdEmpOrgAssignMapper;
    private final MdmEmpEventMapper sysEventMapper;
    private final MdmEmpSrcRefMapper mdEmpSrcRefMapper;
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

        // 3. 数据质检
        boolean idCardMissing = !StringUtils.hasText(basic.getIdCardNo());
        boolean quitWithoutLeave = IhrStaffConstants.STAFF_STATUS_QUIT.equals(basic.getStaffStatus())
                && !StringUtils.hasText(basic.getLeaveDate());
        if (idCardMissing) {
            log.warn("【{}】证件号缺失, 无法建立主档（缺安全存储字段）, ihrStaffId={}, staffNo={}",
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

        // 5. src_ref 匹配主档（替代原 idHash 方案）
        //    先查 src_ref 拿 md_code → 查 mdm_emp；无 src_ref 则主档不存在，走 INSERT
        MdmEmpSrcRefDO srcRef = mdEmpSrcRefMapper.selectByBusinessKey(
                MdmEmpCommonConstants.SRC_SYS_CODE_IHR, SRC_ENTITY_TYPE_STAFF, staffId);
        MdmEmpDO mdEmp = null;
        if (srcRef != null) {
            mdEmp = mdEmpMapper.selectOne(new LambdaQueryWrapper<MdmEmpDO>()
                    .eq(MdmEmpDO::getMdCode, srcRef.getMdCode())
                    .last("LIMIT 1"));
        }
        boolean mdExisted = mdEmp != null;
        LocalDate enrollInDate = MdmEmpUtils.parseDate(basic.getEnrollInDate(), staffId, "enrollInDate");
        LocalDate probationEndDate = MdmEmpUtils.parseDate(basic.getProbationEndDate(), staffId, "probationEndDate");
        LocalDate leaveDate = MdmEmpUtils.parseDate(basic.getLeaveDate(), staffId, "leaveDate");

        if (mdEmp == null) {
            // 首次建档：构造完整主档 DO（证件号明文存储，敏感字段对外查询/导出侧脱敏）
            mdEmp = mdmEmpConverter.toNewMdEmp(basic, IdWorker.getId(), MdmEmpCommonConstants.SRC_SYS_CODE_IHR, enrollInDate);
            mdEmpMapper.insert(mdEmp);
            log.info("【{}】新建主档, mdCode={}, ihrStaffId={}, batchNo={}", BIZ, mdEmp.getMdCode(), staffId, etlBatchNo);
        } else {
            // 已有主档：增量覆盖可变属性字段（mdCode / srcSysCode / firstHireDate 保留）
            mdmEmpConverter.updateBasicInfo(basic, mdEmp);
            mdEmpMapper.updateById(mdEmp);
        }

        // 6. src_ref 幂等插入（主档建立后立即建立映射，保证后续全链路可查）
        mdEmpSrcRefMapper.insertIgnore(IdWorker.getId(), mdEmp.getMdCode(),
                MdmEmpCommonConstants.SRC_SYS_CODE_IHR, SRC_ENTITY_TYPE_STAFF, staffId);

        // 7. 任职拉链判定 + 事件产生
        String eventType = resolveAssignAndEvent(mdEmp, basic, enrollInDate, probationEndDate, leaveDate, mdExisted, etlBatchNo, rowHash);

        log.info("【{}】单员工落库完成, ihrStaffId={}, mdCode={}, eventType={}, mdExisted={}, batchNo={}",
                BIZ, staffId, mdEmp.getMdCode(), eventType, mdExisted, etlBatchNo);
        return MdmEmpIngestResultEnum.INGESTED;
    }

    /**
     * 任职拉链判定（关旧 / 开新 / 更新）并写入变更事件
     *
     * <p>判定规则（按当前有效行 validTo IS NULL）：
     * <ul>
     *     <li>无有效行且离职 + 存在已关链行 → 离职后属性修正：不开新任职行，UPDATE（NORMAL）</li>
     *     <li>无有效行且离职 + 无已关链行 → 首次同步已离职：开完整封口行（validFrom=入职日 / validTo=离职日），LEAVE（HIGH）</li>
     *     <li>无有效行且在职 → 新开任职；主档已存在为 REHIRE，否则 HIRE（HIGH）</li>
     *     <li>有有效行 + 离职 → 关链（validTo=离职日期，状态 ENDED），LEAVE（HIGH）</li>
     *     <li>有有效行 + 部门或职位变更 → 旧行关链（validTo=当天调岗生效日）+ 新开行（validFrom=当天），TRANSFER（NORMAL）</li>
     *     <li>其余属性变更 → 更新当前行组织/职位名，UPDATE（NORMAL）</li>
     * </ul></p>
     *
     * @param mdEmp            主档 DO（已落库，含 mdCode）
     * @param basic            iHR 员工基本信息
     * @param enrollInDate     解析后的入职日期（可为 null）
     * @param probationEndDate 解析后的试用结束日期（可为 null）
     * @param leaveDate        解析后的离职日期（可为 null）
     * @param mdExisted        主档是否已存在（区分 HIRE / REHIRE）
     * @param etlBatchNo       ETL 批次号
     * @param rowHash          行哈希（事件幂等键组成部分）
     * @return 事件类型（HIRE / REHIRE / LEAVE / TRANSFER / UPDATE）
     */
    private String resolveAssignAndEvent(MdmEmpDO mdEmp, IhrStaffBasicInfo basic, LocalDate enrollInDate,
                                         LocalDate probationEndDate, LocalDate leaveDate,
                                         boolean mdExisted, String etlBatchNo, String rowHash) {
        String staffId = basic.getStaffId();
        Long mdCode = mdEmp.getMdCode();
        String orgCode = MdmEmpUtils.longToStringSafe(basic.getDepartmentId());
        String orgName = basic.getDepartmentName();
        String positionCode = basic.getPositionId();
        String positionName = basic.getPositionName();
        boolean quit = IhrStaffConstants.STAFF_STATUS_QUIT.equals(basic.getStaffStatus());

        MdmEmpOrgAssignDO active = mdEmpOrgAssignMapper.selectOne(new LambdaQueryWrapper<MdmEmpOrgAssignDO>()
                .eq(MdmEmpOrgAssignDO::getMdCode, mdCode)
                .isNull(MdmEmpOrgAssignDO::getValidTo)
                .last("LIMIT 1"));

        String eventType;
        if (active == null && quit) {
            // 无当前有效行 + 离职状态：补查最近一条已关链行，区分属性修正与首次同步
            MdmEmpOrgAssignDO lastEnded = mdEmpOrgAssignMapper.selectOne(new LambdaQueryWrapper<MdmEmpOrgAssignDO>()
                    .eq(MdmEmpOrgAssignDO::getMdCode, mdCode)
                    .isNotNull(MdmEmpOrgAssignDO::getValidTo)
                    .orderByDesc(MdmEmpOrgAssignDO::getValidTo)
                    .last("LIMIT 1"));
            if (lastEnded != null) {
                // 离职后属性修正：不开新任职行（避免误判 REHIRE 与矛盾行），仅发 UPDATE 事件
                log.info("【{}】离职员工属性修正, 不新开任职行, ihrStaffId={}, mdCode={}, batchNo={}", BIZ, staffId, mdCode, etlBatchNo);
                eventType = MdmEmpEventConstants.EVENT_TYPE_UPDATE;
                insertEvent(mdEmp, basic, eventType, MdmEmpEventConstants.CHANGE_LEVEL_NORMAL,
                        lastEnded.getOrgCode(), lastEnded.getOrgName(), lastEnded.getPositionCode(), lastEnded.getPositionName(),
                        lastEnded.getValidFrom(), lastEnded.getValidTo(), etlBatchNo, rowHash);
            } else {
                // 首次同步即离职：开一行完整封口的任职行（validFrom=入职日, validTo=离职日）
                insertAssign(mdCode, basic, enrollInDate, probationEndDate, leaveDate, leaveDate, staffId);
                eventType = MdmEmpEventConstants.EVENT_TYPE_LEAVE;
                insertEvent(mdEmp, basic, eventType, MdmEmpEventConstants.CHANGE_LEVEL_HIGH,
                        orgCode, orgName, positionCode, positionName, enrollInDate, leaveDate, etlBatchNo, rowHash);
            }
        } else if (active == null) {
            // 新开任职（首次建档或再入职）
            insertAssign(mdCode, basic, enrollInDate, probationEndDate, leaveDate, null, staffId);
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
            // 调岗：以当天为调岗生效日，旧行关链 + 新开行（iHR 无调岗日期字段，SCD2 以批次日为界）
            LocalDate transferDate = LocalDate.now();
            active.setValidTo(transferDate);
            active.setAssignStatus(MdmEmpOrgAssignConstants.ASSIGN_STATUS_ENDED);
            mdEmpOrgAssignMapper.updateById(active);
            insertAssign(mdCode, basic, transferDate, probationEndDate, leaveDate, null, staffId);
            eventType = MdmEmpEventConstants.EVENT_TYPE_TRANSFER;
            insertEvent(mdEmp, basic, eventType, MdmEmpEventConstants.CHANGE_LEVEL_NORMAL,
                    orgCode, orgName, positionCode, positionName, transferDate, null, etlBatchNo, rowHash);
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
     * 新开任职行（validTo 为 NULL 表示当前有效；封口场景显式传入 validTo）
     *
     * @param mdCode           人员主数据编码
     * @param basic            iHR 员工基本信息（Converter 复用 departmentId / departmentName / positionId / positionName）
     * @param validFrom        任职生效起始日（入职场景=入职日期；调岗场景=调岗生效日）
     * @param probationEndDate 解析后的试用结束日期
     * @param leaveDate        解析后的离职日期
     * @param validTo          任职生效截止日；null 表示当前有效（首次同步已离职场景传离职日形成完整封口行）
     * @param staffId          员工 ID（日志定位用）
     */
    private void insertAssign(Long mdCode, IhrStaffBasicInfo basic, LocalDate validFrom,
                              LocalDate probationEndDate, LocalDate leaveDate, LocalDate validTo, String staffId) {
        MdmEmpOrgAssignDO assign = mdmEmpConverter.toAssign(basic, mdCode,
                deriveAssignStatus(basic.getStaffStatus(), validFrom, probationEndDate, leaveDate),
                MdmEmpOrgAssignConstants.PRINCIPAL_FLAG_YES, validFrom);
        if (validTo != null) {
            assign.setValidTo(validTo);
        }
        mdEmpOrgAssignMapper.insert(assign);
    }

    /**
     * 写入变更事件（幂等键冲突时静默吞掉）
     *
     * <p>eventId = SHA-256(mdCode:eventType:rowHash)：内容不变的事件重复产生时
     * 幂等去重，真实内容变化（rowHash 变化）则生成新事件。</p>
     */
    private void insertEvent(MdmEmpDO mdEmp, IhrStaffBasicInfo basic, String eventType, String changeLevel,
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
        MdmEmpEventDO event = new MdmEmpEventDO();
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
     * 构造行哈希 —— SHA-256(工号|姓名|手机号|个人邮箱|工作邮箱|性别|生日|学历|证件类型|
     * 部门|职位|状态|入职|离职)
     *
     * <p>覆盖「人属性 + 任职 + 状态 + 证件」的关键字段集合，任一变化即视为行变更；
     * null 统一按空串参与拼接，保证哈希稳定。</p>
     */
    private String buildRowHash(IhrStaffBasicInfo basic) {
        String joined = String.join(FIELD_SEP,
                MdmEmpUtils.nullToEmpty(basic.getStaffNo()),
                MdmEmpUtils.nullToEmpty(basic.getStaffName()),
                MdmEmpUtils.nullToEmpty(basic.getMobileNo()),
                MdmEmpUtils.nullToEmpty(basic.getEmail()),
                MdmEmpUtils.nullToEmpty(basic.getWorkEmail()),
                MdmEmpUtils.nullToEmpty(basic.getSex()),
                MdmEmpUtils.nullToEmpty(basic.getBirthday()),
                MdmEmpUtils.nullToEmpty(basic.getHighestEducation()),
                MdmEmpUtils.nullToEmpty(basic.getIdCardType()),
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
