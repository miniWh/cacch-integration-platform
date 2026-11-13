package com.cacch.integration.service.mdm.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.constant.mdm.MdmDeptCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmDeptEventConstants;
import com.cacch.integration.common.constant.mdm.MdmDeptLogConstants;
import com.cacch.integration.common.enums.mdm.MdmDeptIngestResultEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.common.utils.mdm.MdmDeptUtils;
import com.cacch.integration.entity.mdm.MdmDeptDO;
import com.cacch.integration.entity.mdm.MdmDeptEventDO;
import com.cacch.integration.entity.mdm.OdsIhrDeptDO;
import com.cacch.integration.entity.mdm.StgIhrDeptDO;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.mapper.mdm.MdmDeptEventMapper;
import com.cacch.integration.mapper.mdm.MdmDeptMapper;
import com.cacch.integration.mapper.mdm.OdsIhrDeptMapper;
import com.cacch.integration.mapper.mdm.StgIhrDeptMapper;
import com.cacch.integration.service.mdm.api.IMdmDeptIngestService;
import com.cacch.integration.service.mdm.convert.MdmDeptConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门主数据（MDM）单部门落库 Service 实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmDeptIngestServiceImpl implements IMdmDeptIngestService {

    private static final String BIZ = MdmDeptLogConstants.LOG_BIZ;

    /**
     * rowHash / eventId 构造用字段分隔符
     */
    private static final String FIELD_SEP = "|";

    /**
     * 静态 ObjectMapper —— 仅用于事件 payload 序列化
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final StgIhrDeptMapper stgIhrDeptMapper;
    private final OdsIhrDeptMapper odsIhrDeptMapper;
    private final MdmDeptMapper mdmDeptMapper;
    private final MdmDeptEventMapper mdmDeptEventMapper;
    private final MdmDeptConverter mdmDeptConverter;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public MdmDeptIngestResultEnum ingestOne(IhrDepartment dept, String rawJson, String etlBatchNo) {
        String deptUuid = dept.getUuid();

        // 1. stg 贴源（同批次重复报文幂等吞掉）
        StgIhrDeptDO stg = new StgIhrDeptDO();
        stg.setId(IdWorker.getId());
        stg.setEtlBatchNo(etlBatchNo);
        stg.setIhrDeptUuid(deptUuid);
        stg.setRawJson(rawJson);
        stg.setHttpStatus(HttpStatus.OK.value());
        stgIhrDeptMapper.insertIgnore(stg);

        // 2. rowHash 比对 —— 未变化则幂等短路
        String rowHash = buildRowHash(dept);
        OdsIhrDeptDO existing = odsIhrDeptMapper.selectOne(new LambdaQueryWrapper<OdsIhrDeptDO>()
                .eq(OdsIhrDeptDO::getIhrDeptUuid, deptUuid)
                .last("LIMIT 1"));
        if (existing != null && rowHash.equals(existing.getRowHash())) {
            log.info("【{}】ods 数据无变化, 幂等跳过后续处理, ihrDeptUuid={}, batchNo={}", BIZ, deptUuid, etlBatchNo);
            return MdmDeptIngestResultEnum.SKIPPED_UNCHANGED;
        }

        // 3. ods 规范化 upsert（verNo 递增；dqFlag 当前恒 0，部门域暂无致命质检项）
        OdsIhrDeptDO ods = mdmDeptConverter.toOds(dept, IdWorker.getId(), rowHash,
                existing == null || existing.getVerNo() == null ? 1 : existing.getVerNo() + 1);
        odsIhrDeptMapper.upsert(ods);

        // 4. mdm_dept 主档 upsert（按 deptUuid 识别唯一部门；parentDeptCode 存编码不做自引用，乱序建档安全）
        MdmDeptDO mdDept = mdmDeptMapper.selectOne(new LambdaQueryWrapper<MdmDeptDO>()
                .eq(MdmDeptDO::getDeptUuid, deptUuid)
                .last("LIMIT 1"));
        boolean mdExisted = mdDept != null;
        String eventType;
        String changeLevel;
        if (mdDept == null) {
            mdDept = mdmDeptConverter.toNewMdDept(dept, MdmDeptCommonConstants.SRC_SYS_CODE_IHR);
            mdmDeptMapper.insert(mdDept);
            eventType = MdmDeptEventConstants.EVENT_TYPE_CREATE;
            changeLevel = MdmDeptEventConstants.CHANGE_LEVEL_HIGH;
            log.info("【{}】新建部门主档, deptId={}, ihrDeptUuid={}, batchNo={}", BIZ, mdDept.getId(), deptUuid, etlBatchNo);
        } else {
            mdmDeptConverter.updateBasicInfo(dept, mdDept);
            mdmDeptMapper.updateById(mdDept);
            eventType = MdmDeptEventConstants.EVENT_TYPE_UPDATE;
            changeLevel = MdmDeptEventConstants.CHANGE_LEVEL_NORMAL;
        }

        // 5. 事件幂等插入
        insertEvent(mdDept, eventType, changeLevel, etlBatchNo, rowHash);

        log.info("【{}】单部门落库完成, ihrDeptUuid={}, deptId={}, eventType={}, mdExisted={}, batchNo={}",
                BIZ, deptUuid, mdDept.getId(), eventType, mdExisted, etlBatchNo);
        return MdmDeptIngestResultEnum.INGESTED;
    }

    /**
     * 停用比对 —— mdm_dept 中不在 iHR 存活集合内的部门置停用并写 DISABLE/HIGH 事件
     *
     * <p>逐条处理未逻辑删除行：uuid 不在存活集且状态非 DISABLE 的置
     * {@code dept_status='DISABLE'} 并写事件（eventId =
     * SHA-256(deptUuid:DISABLE:etlBatchNo)，同批次幂等）；
     * 已停用的跳过并汇总计数。</p>
     *
     * @param aliveDeptUuids 本轮 iHR 全量拉取到的部门 uuid 集合
     * @param etlBatchNo     ETL 批次号（事件幂等键组成部分）
     * @return 本轮实际停用的部门数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public int disableMissing(Collection<String> aliveDeptUuids, String etlBatchNo) {
        if (CollectionUtils.isEmpty(aliveDeptUuids)) {
            log.info("【{}】存活部门集合为空, 跳过停用比对, reason=防止误停用全量, batchNo={}", BIZ, etlBatchNo);
            return 0;
        }
        List<MdmDeptDO> mdmList = mdmDeptMapper.selectList(null);
        int disabled = 0;
        int alreadyDisabled = 0;
        for (MdmDeptDO mdDept : mdmList) {
            if (aliveDeptUuids.contains(mdDept.getDeptUuid())) {
                continue;
            }
            if (MdmDeptCommonConstants.DEPT_STATUS_DISABLED.equals(mdDept.getDeptStatus())) {
                alreadyDisabled++;
                continue;
            }
            mdDept.setDeptStatus(MdmDeptCommonConstants.DEPT_STATUS_DISABLED);
            mdmDeptMapper.updateById(mdDept);
            insertEvent(mdDept, MdmDeptEventConstants.EVENT_TYPE_DISABLE,
                    MdmDeptEventConstants.CHANGE_LEVEL_HIGH, etlBatchNo, etlBatchNo);
            disabled++;
            log.info("【{}】部门停用完成, deptId={}, ihrDeptUuid={}, deptCode={}, batchNo={}",
                    BIZ, mdDept.getId(), mdDept.getDeptUuid(), mdDept.getDeptCode(), etlBatchNo);
        }
        log.info("【{}】停用比对完成, 停用数={}, 既往已停用数={}, 存活集大小={}, batchNo={}",
                BIZ, disabled, alreadyDisabled, aliveDeptUuids.size(), etlBatchNo);
        return disabled;
    }

    /**
     * 按 iHR 部门 ID 解析部门编码（ods_ihr_dept.ihr_dept_id → department_code）
     *
     * @param ihrDeptId iHR 部门 ID（数字字符串形式；null 或空时直接返回 null）
     * @return 部门编码；未匹配或编码为空时返回 null
     */
    @Override
    public String resolveDeptCodeByIhrDeptId(String ihrDeptId) {
        if (!StringUtils.hasText(ihrDeptId)) {
            return null;
        }
        OdsIhrDeptDO odsDept = odsIhrDeptMapper.selectOne(new LambdaQueryWrapper<OdsIhrDeptDO>()
                .eq(OdsIhrDeptDO::getIhrDeptId, ihrDeptId)
                .last("LIMIT 1"));
        return odsDept == null ? null : MdmDeptUtils.nullIfBlank(odsDept.getDepartmentCode());
    }

    /**
     * 部门主数据一致性核对（mdm_dept 与 ods_ihr_dept 双向 uuid 差集，纯查询无事务）
     */
    @Override
    public DeptVerifyResult verifyWithOds() {
        List<MdmDeptDO> mdmList = mdmDeptMapper.selectList(null);
        List<OdsIhrDeptDO> odsList = odsIhrDeptMapper.selectList(null);

        Set<String> mdmUuids = new HashSet<>();
        for (MdmDeptDO md : mdmList) {
            mdmUuids.add(md.getDeptUuid());
        }
        Set<String> odsUuids = new HashSet<>();
        for (OdsIhrDeptDO ods : odsList) {
            odsUuids.add(ods.getIhrDeptUuid());
        }

        List<String> missingInMdm = new ArrayList<>(odsUuids);
        missingInMdm.removeAll(mdmUuids);
        List<String> missingInOds = new ArrayList<>(mdmUuids);
        missingInOds.removeAll(odsUuids);
        return new DeptVerifyResult(mdmList.size(), odsList.size(), missingInMdm, missingInOds);
    }

    /**
     * 写入变更事件（幂等键冲突时静默吞掉）
     *
     * <p>eventId = SHA-256(deptUuid:eventType:idBasis)：落库场景 idBasis 为 rowHash
     * （内容不变的事件重复产生时幂等去重）；停用场景 idBasis 为 etlBatchNo
     * （同批次同部门仅一条 DISABLE 事件）。</p>
     *
     * @param mdDept      部门主档 DO（已落库，含 id）
     * @param eventType   事件类型（CREATE / UPDATE / DISABLE）
     * @param changeLevel 变更级别（HIGH / NORMAL）
     * @param etlBatchNo  ETL 批次号
     * @param idBasis     事件幂等键基础串（落库场景传 rowHash，停用场景传 etlBatchNo）
     */
    private void insertEvent(MdmDeptDO mdDept, String eventType, String changeLevel,
                             String etlBatchNo, String idBasis) {
        Long deptId = mdDept.getId();
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("deptId", deptId);
        payloadMap.put("deptUuid", mdDept.getDeptUuid());
        payloadMap.put("deptCode", mdDept.getDeptCode());
        payloadMap.put("deptName", mdDept.getDeptName());
        payloadMap.put("etlBatchNo", etlBatchNo);

        String payload;
        try {
            payload = MAPPER.writeValueAsString(payloadMap);
        } catch (Exception e) {
            log.error("【{}】事件 payload 序列化失败, deptId={}, eventType={}", BIZ, deptId, eventType, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "事件 payload 序列化失败");
        }

        String eventId = MdmDeptUtils.sha256Hex(mdDept.getDeptUuid() + ":" + eventType + ":" + idBasis);
        MdmDeptEventDO event = new MdmDeptEventDO();
        event.setId(IdWorker.getId());
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setChangeLevel(changeLevel);
        event.setDeptId(deptId);
        event.setDeptCode(mdDept.getDeptCode());
        event.setPayload(payload);
        event.setEtlBatchNo(etlBatchNo);
        event.setStatus(MdmDeptEventConstants.EVENT_STATUS_PENDING);
        int rows = mdmDeptEventMapper.insertIgnore(event);
        if (rows == 0) {
            log.info("【{}】事件已存在, 幂等跳过, deptId={}, eventType={}, eventId={}",
                    BIZ, deptId, eventType, eventId);
        }
    }

    /**
     * 构造行哈希 —— SHA-256(名称|上级部门ID|上级部门编码|类型|部门编码|门店编号|
     * 负责人|状态|排序号|是否虚拟)
     *
     * <p>覆盖部门档案的关键字段集合，任一变化即视为行变更；
     * null 统一按空串参与拼接，保证哈希稳定；
     * 排除 lastUpdate / createdDate 等 iHR 侧技术时间戳，避免无业务含义的哈希漂移。</p>
     *
     * @param dept iHR 部门实体
     * @return 64 位小写 hex 行哈希
     */
    private String buildRowHash(IhrDepartment dept) {
        String joined = String.join(FIELD_SEP,
                MdmDeptUtils.nullToEmpty(dept.getName()),
                MdmDeptUtils.nullToEmpty(dept.getParentId()),
                MdmDeptUtils.nullToEmpty(dept.getParentDepartmentCode()),
                MdmDeptUtils.nullToEmpty(dept.getType()),
                MdmDeptUtils.nullToEmpty(dept.getDepartmentCode()),
                MdmDeptUtils.nullToEmpty(dept.getStoreNumber()),
                MdmDeptUtils.nullToEmpty(dept.getPrincipalStaffId()),
                MdmDeptUtils.nullToEmpty(dept.getDepartmentStatus()),
                dept.getSequence() == null ? "" : String.valueOf(dept.getSequence()),
                dept.getVirtual() == null ? "" : String.valueOf(dept.getVirtual()));
        return MdmDeptUtils.sha256Hex(joined);
    }
}
