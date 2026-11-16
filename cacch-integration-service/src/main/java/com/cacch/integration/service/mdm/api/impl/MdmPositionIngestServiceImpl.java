package com.cacch.integration.service.mdm.api.impl;

import com.cacch.integration.common.constant.mdm.MdmCommonConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.constant.mdm.MdmDeptCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmPositionCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEventConstants;
import com.cacch.integration.common.constant.mdm.MdmPositionLogConstants;
import com.cacch.integration.common.enums.mdm.MdmIngestResultEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.common.utils.mdm.MdmUtils;
import com.cacch.integration.entity.mdm.MdmDeptSrcRefDO;
import com.cacch.integration.entity.mdm.MdmPositionDO;
import com.cacch.integration.entity.mdm.MdmPositionEventDO;
import com.cacch.integration.entity.mdm.MdmPositionSrcRefDO;
import com.cacch.integration.entity.mdm.OdsIhrDeptDO;
import com.cacch.integration.entity.mdm.OdsIhrPositionDO;
import com.cacch.integration.entity.mdm.StgIhrPositionDO;
import com.cacch.integration.integration.ihr.client.dto.IhrPosition;
import com.cacch.integration.mapper.mdm.MdmDeptSrcRefMapper;
import com.cacch.integration.mapper.mdm.MdmPositionEventMapper;
import com.cacch.integration.mapper.mdm.MdmPositionMapper;
import com.cacch.integration.mapper.mdm.MdmPositionSrcRefMapper;
import com.cacch.integration.mapper.mdm.OdsIhrDeptMapper;
import com.cacch.integration.mapper.mdm.OdsIhrPositionMapper;
import com.cacch.integration.mapper.mdm.StgIhrPositionMapper;
import com.cacch.integration.service.mdm.api.IMdmPositionIngestService;
import com.cacch.integration.service.mdm.convert.MdmPositionConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 岗位主数据（MDM）单岗位落库 Service 实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmPositionIngestServiceImpl implements IMdmPositionIngestService {

    private static final String BIZ = MdmPositionLogConstants.LOG_BIZ;

    /**
     * rowHash / eventId 构造用字段分隔符
     */
    private static final String FIELD_SEP = "|";

    /**
     * 静态 ObjectMapper —— 仅用于 positionScope 序列化与事件 payload 序列化
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final StgIhrPositionMapper stgIhrPositionMapper;
    private final OdsIhrPositionMapper odsIhrPositionMapper;
    private final MdmPositionMapper mdmPositionMapper;
    private final MdmPositionEventMapper mdmPositionEventMapper;
    private final MdmPositionSrcRefMapper mdmPositionSrcRefMapper;
    private final MdmPositionConverter mdmPositionConverter;
    private final OdsIhrDeptMapper odsIhrDeptMapper;
    private final MdmDeptSrcRefMapper mdmDeptSrcRefMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public MdmIngestResultEnum ingestOne(IhrPosition position, String rawJson, String etlBatchNo) {
        String positionUuid = position.getId();

        // 0. 部门 ID 翻译：iHR departmentId(Long) → mdm_dept.id(BIGINT)，主档写入阶段使用
        Long mdmDeptId = translateDeptId(position.getDepartmentId());

        // 1. rowHash 比对 —— ODS 职责是完整源系统快照，哈希基于 ODS 侧字段（含 iHR 原始部门 ID）
        String positionScopeJson = toJson(position.getPositionScope());
        String rowHash = buildRowHash(position, positionScopeJson);
        OdsIhrPositionDO existing = odsIhrPositionMapper.selectOne(new LambdaQueryWrapper<OdsIhrPositionDO>()
                .eq(OdsIhrPositionDO::getIhrPositionUuid, positionUuid)
                .last("LIMIT 1"));
        if (existing != null && rowHash.equals(existing.getRowHash())) {
            log.info("【{}】ods 数据无变化, 幂等跳过后续处理（含贴源）, ihrPositionUuid={}, batchNo={}",
                    BIZ, positionUuid, etlBatchNo);
            return MdmIngestResultEnum.SKIPPED_UNCHANGED;
        }

        // 2. stg 贴源（仅变更数据进入；同批次重复报文幂等吞掉）
        StgIhrPositionDO stg = new StgIhrPositionDO();
        stg.setId(IdWorker.getId());
        stg.setEtlBatchNo(etlBatchNo);
        stg.setIhrPositionUuid(positionUuid);
        stg.setRawJson(rawJson);
        stg.setHttpStatus(HttpStatus.OK.value());
        stgIhrPositionMapper.insertIgnore(stg);

        // 3. ods 规范化 upsert（verNo 递增；departmentId 存 iHR 原始 Long 型部门 ID — ODS 职责）
        OdsIhrPositionDO ods = mdmPositionConverter.toOds(position, IdWorker.getId(), rowHash,
                existing == null || existing.getVerNo() == null ? 1 : existing.getVerNo() + 1,
                positionScopeJson);
        odsIhrPositionMapper.upsert(ods);

        // 4. mdm_position 主档 upsert（经 src_ref 定位唯一岗位；departmentId 存翻译后的 mdm_dept.id，
        //    纯主数据语义；parentPositionId 存 UUID 不做自引用，乱序建档安全）
        MdmPositionSrcRefDO srcRef = mdmPositionSrcRefMapper.selectByBusinessKey(
                MdmCommonConstants.SRC_SYS_CODE_IHR, MdmPositionCommonConstants.SRC_ENTITY_TYPE_POSITION,
                positionUuid);
        MdmPositionDO mdPosition = null;
        if (srcRef != null) {
            mdPosition = mdmPositionMapper.selectById(srcRef.getPositionId());
        }
        boolean mdExisted = mdPosition != null;
        String eventType;
        String changeLevel;
        if (mdPosition == null) {
            mdPosition = mdmPositionConverter.toNewMdPosition(position, MdmCommonConstants.SRC_SYS_CODE_IHR,
                    mdmDeptId);
            mdmPositionMapper.insert(mdPosition);
            eventType = MdmEventConstants.EVENT_TYPE_CREATE;
            changeLevel = MdmEventConstants.CHANGE_LEVEL_HIGH;
            log.info("【{}】新建岗位主档, positionId={}, ihrPositionUuid={}, batchNo={}",
                    BIZ, mdPosition.getId(), positionUuid, etlBatchNo);
        } else {
            mdmPositionConverter.updateBasicInfo(position, mdmDeptId, mdPosition);
            mdmPositionMapper.updateById(mdPosition);
            eventType = MdmEventConstants.EVENT_TYPE_UPDATE;
            changeLevel = MdmEventConstants.CHANGE_LEVEL_NORMAL;
        }

        // 4.1 src_ref 幂等插入（主档建立后立即建立来源映射，保证后续定位与多源追溯可查）
        mdmPositionSrcRefMapper.insertIgnore(IdWorker.getId(), mdPosition.getId(),
                MdmCommonConstants.SRC_SYS_CODE_IHR, MdmPositionCommonConstants.SRC_ENTITY_TYPE_POSITION,
                positionUuid);

        // 5. 事件幂等插入
        insertEvent(mdPosition, positionUuid, eventType, changeLevel, etlBatchNo, rowHash);

        log.info("【{}】单岗位落库完成, ihrPositionUuid={}, positionId={}, eventType={}, mdExisted={}, batchNo={}",
                BIZ, positionUuid, mdPosition.getId(), eventType, mdExisted, etlBatchNo);
        return MdmIngestResultEnum.INGESTED;
    }

    /**
     * 停用比对 —— mdm_position 中不在 iHR 存活集合内的岗位置停用并写 DISABLE/HIGH 事件
     *
     * <p>经 src_ref（IHR / POSITION）反查各主档行的 iHR uuid 后与存活集比对：
     * 无 iHR 映射的岗位不受本轮停用比对管辖（多源保护）；
     * uuid 不在存活集且状态非 DISABLE 的置 {@code position_state='DISABLE'} 并写事件
     * （eventId = SHA-256(positionUuid:DISABLE:etlBatchNo)，同批次幂等）；
     * 已停用的跳过并汇总计数。</p>
     *
     * @param alivePositionUuids 本轮 iHR 全量拉取到的岗位 uuid 集合
     * @param etlBatchNo         ETL 批次号（事件幂等键组成部分）
     * @return 本轮实际停用的岗位数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public int disableMissing(Collection<String> alivePositionUuids, String etlBatchNo) {
        if (CollectionUtils.isEmpty(alivePositionUuids)) {
            log.info("【{}】存活岗位集合为空, 跳过停用比对, reason=防止误停用全量, batchNo={}", BIZ, etlBatchNo);
            return 0;
        }
        Map<Long, String> uuidByPositionId = loadIhrUuidMap();
        List<MdmPositionDO> mdmList = mdmPositionMapper.selectList(null);
        int disabled = 0;
        int alreadyDisabled = 0;
        for (MdmPositionDO mdPosition : mdmList) {
            String positionUuid = uuidByPositionId.get(mdPosition.getId());
            if (positionUuid == null) {
                log.info("【{}】岗位无 iHR 来源映射, 跳过停用比对, positionId={}, positionCode={}",
                        BIZ, mdPosition.getId(), mdPosition.getPositionCode());
                continue;
            }
            if (alivePositionUuids.contains(positionUuid)) {
                continue;
            }
            if (MdmPositionCommonConstants.POSITION_STATUS_DISABLED.equals(mdPosition.getPositionState())) {
                alreadyDisabled++;
                continue;
            }
            mdPosition.setPositionState(MdmPositionCommonConstants.POSITION_STATUS_DISABLED);
            mdmPositionMapper.updateById(mdPosition);
            insertEvent(mdPosition, positionUuid, MdmEventConstants.EVENT_TYPE_DISABLE,
                    MdmEventConstants.CHANGE_LEVEL_HIGH, etlBatchNo, etlBatchNo);
            disabled++;
            log.info("【{}】岗位停用完成, positionId={}, ihrPositionUuid={}, positionCode={}, batchNo={}",
                    BIZ, mdPosition.getId(), positionUuid, mdPosition.getPositionCode(), etlBatchNo);
        }
        log.info("【{}】停用比对完成, 停用数={}, 既往已停用数={}, 存活集大小={}, batchNo={}",
                BIZ, disabled, alreadyDisabled, alivePositionUuids.size(), etlBatchNo);
        return disabled;
    }

    /**
     * 加载 iHR 域 src_ref 映射（positionId → iHR uuid，自动过滤逻辑删除）
     *
     * @return positionId → iHR 岗位 uuid 映射；无映射时返回空 Map
     */
    private Map<Long, String> loadIhrUuidMap() {
        List<MdmPositionSrcRefDO> refs = mdmPositionSrcRefMapper.selectList(new LambdaQueryWrapper<MdmPositionSrcRefDO>()
                .eq(MdmPositionSrcRefDO::getSrcSysCode, MdmCommonConstants.SRC_SYS_CODE_IHR)
                .eq(MdmPositionSrcRefDO::getSrcEntityType, MdmPositionCommonConstants.SRC_ENTITY_TYPE_POSITION));
        Map<Long, String> map = new HashMap<>(refs.size() * 2);
        for (MdmPositionSrcRefDO ref : refs) {
            map.put(ref.getPositionId(), ref.getSrcEntityId());
        }
        return map;
    }

    /**
     * 岗位主数据一致性核对（mdm_position 与 ods_ihr_position 双向 uuid 差集，纯查询无事务）
     *
     * <p>mdm 侧 uuid 集合取自 iHR 域 src_ref（多源映射），ods 侧取自
     * ods_ihr_position.ihr_position_uuid；双向差集为空视为一致。</p>
     */
    @Override
    public PositionVerifyResult verifyWithOds() {
        Map<Long, String> uuidByPositionId = loadIhrUuidMap();
        List<OdsIhrPositionDO> odsList = odsIhrPositionMapper.selectList(null);

        Set<String> mdmUuids = new HashSet<>(uuidByPositionId.values());
        Set<String> odsUuids = new HashSet<>();
        for (OdsIhrPositionDO ods : odsList) {
            odsUuids.add(ods.getIhrPositionUuid());
        }

        List<String> missingInMdm = new ArrayList<>(odsUuids);
        missingInMdm.removeAll(mdmUuids);
        List<String> missingInOds = new ArrayList<>(mdmUuids);
        missingInOds.removeAll(odsUuids);
        return new PositionVerifyResult(uuidByPositionId.size(), odsList.size(), missingInMdm, missingInOds);
    }

    /**
     * 写入变更事件（幂等键冲突时静默吞掉）
     *
     * <p>eventId = SHA-256(positionUuid:eventType:idBasis)：落库场景 idBasis 为 rowHash
     * （内容不变的事件重复产生时幂等去重）；停用场景 idBasis 为 etlBatchNo
     * （同批次同岗位仅一条 DISABLE 事件）。</p>
     *
     * @param mdPosition   岗位主档 DO（已落库，含 id）
     * @param positionUuid iHR 岗位 uuid（来源标识，经入参传递——主档本体不再存储）
     * @param eventType    事件类型（CREATE / UPDATE / DISABLE）
     * @param changeLevel  变更级别（HIGH / NORMAL）
     * @param etlBatchNo   ETL 批次号
     * @param idBasis      事件幂等键基础串（落库场景传 rowHash，停用场景传 etlBatchNo）
     */
    private void insertEvent(MdmPositionDO mdPosition, String positionUuid, String eventType, String changeLevel,
                             String etlBatchNo, String idBasis) {
        Long positionId = mdPosition.getId();
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("positionId", positionId);
        payloadMap.put("positionUuid", positionUuid);
        payloadMap.put("positionCode", mdPosition.getPositionCode());
        payloadMap.put("positionName", mdPosition.getPositionName());
        payloadMap.put("etlBatchNo", etlBatchNo);

        String payload;
        try {
            payload = MAPPER.writeValueAsString(payloadMap);
        } catch (Exception e) {
            log.error("【{}】事件 payload 序列化失败, positionId={}, eventType={}", BIZ, positionId, eventType, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "事件 payload 序列化失败");
        }

        String eventId = MdmUtils.sha256Hex(positionUuid + ":" + eventType + ":" + idBasis);
        MdmPositionEventDO event = new MdmPositionEventDO();
        event.setId(IdWorker.getId());
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setChangeLevel(changeLevel);
        event.setPositionId(positionId);
        event.setPositionCode(mdPosition.getPositionCode());
        event.setPayload(payload);
        event.setEtlBatchNo(etlBatchNo);
        event.setStatus(MdmEventConstants.EVENT_STATUS_PENDING);
        int rows = mdmPositionEventMapper.insertIgnore(event);
        if (rows == 0) {
            log.info("【{}】事件已存在, 幂等跳过, positionId={}, eventType={}, eventId={}",
                    BIZ, positionId, eventType, eventId);
        }
    }

    /**
     * 构造行哈希 —— SHA-256(名称|公司ID|简称|职位编号|应用范围|编制人数|生效日期|所属部门ID|
     * 所属部门名称|职位描述|职务ID|职务名称|职级ID|职级名称|任职资格|父级编号|是否职位组|
     * 职位状态|应用范围部门列表)
     *
     * <p>覆盖岗位档案的关键字段集合，任一变化即视为行变更；
     * null 统一按空串参与拼接，保证哈希稳定；
     * 排除 positionStateString / updatedDate / createdDate 等描述冗余与 iHR 侧技术时间戳，
     * 避免无业务含义的哈希漂移。</p>
     *
     * @param position          iHR 岗位实体
     * @param positionScopeJson 应用范围部门 ID 列表 JSON 字符串（与 ODS 落库值同源，保证跨接口稳定）
     * @return 64 位小写 hex 行哈希
     */
    private String buildRowHash(IhrPosition position, String positionScopeJson) {
        // ODS 职责：完整源系统快照 —— departmentId 参与哈希用 iHR 原始 Long 值
        String joined = String.join(FIELD_SEP,
                MdmUtils.nullToEmpty(position.getPositionName()),
                MdmUtils.nullToEmpty(position.getCompanyId()),
                MdmUtils.nullToEmpty(position.getAbbreviation()),
                MdmUtils.nullToEmpty(position.getPositionCode()),
                MdmUtils.nullToEmpty(position.getAppliedRange()),
                position.getCapacity() == null ? "" : String.valueOf(position.getCapacity()),
                MdmUtils.nullToEmpty(position.getEffectiveDate()),
                position.getDepartmentId() == null ? "" : String.valueOf(position.getDepartmentId()),
                MdmUtils.nullToEmpty(position.getDepartmentName()),
                MdmUtils.nullToEmpty(position.getDescription()),
                MdmUtils.nullToEmpty(position.getJobTitleId()),
                MdmUtils.nullToEmpty(position.getJobTitleName()),
                MdmUtils.nullToEmpty(position.getPositionGradeId()),
                MdmUtils.nullToEmpty(position.getPositionGradeName()),
                MdmUtils.nullToEmpty(position.getQualifications()),
                MdmUtils.nullToEmpty(position.getParentId()),
                position.getIsPositionGroup() == null ? "" : String.valueOf(position.getIsPositionGroup()),
                MdmUtils.nullToEmpty(position.getPositionState()),
                MdmUtils.nullToEmpty(positionScopeJson));
        return MdmUtils.sha256Hex(joined);
    }

    /**
     * 翻译 iHR departmentId → mdm_dept.id
     *
     * <p>路径：iHR departmentId（Long → String）→ ods_ihr_dept.ihr_dept_id 命中 → ihr_dept_uuid →
     * mdm_dept_src_ref（IHR / DEPT / ihrDeptUuid）→ mdm_dept.id。
     * 翻译失败（部门未同步 / iHR 无该部门 / 无主档映射）时返回 null，不抛异常——
     * 岗位落库降级继续（department_id 置 null，department_ihr_id 独立保留溯源）。</p>
     *
     * @param ihrDeptIdLong iHR 原始部门 ID（Long；null 或无有效值时直接返回 null）
     * @return mdm_dept.id（BIGINT）；未命中时为 null
     */
    private Long translateDeptId(Long ihrDeptIdLong) {
        if (ihrDeptIdLong == null) {
            return null;
        }
        String ihrDeptIdStr = String.valueOf(ihrDeptIdLong);
        OdsIhrDeptDO odsDept = odsIhrDeptMapper.selectOne(new LambdaQueryWrapper<OdsIhrDeptDO>()
                .eq(OdsIhrDeptDO::getIhrDeptId, ihrDeptIdStr)
                .last("LIMIT 1"));
        if (odsDept == null || odsDept.getIhrDeptUuid() == null || odsDept.getIhrDeptUuid().isEmpty()) {
            log.info("【{}】部门翻译终止, ihrDeptId={}, reason=ods_ihr_dept 未命中", BIZ, ihrDeptIdStr);
            return null;
        }
        MdmDeptSrcRefDO deptRef = mdmDeptSrcRefMapper.selectByBusinessKey(
                MdmCommonConstants.SRC_SYS_CODE_IHR, MdmDeptCommonConstants.SRC_ENTITY_TYPE_DEPT,
                odsDept.getIhrDeptUuid());
        if (deptRef == null) {
            log.info("【{}】部门翻译终止, ihrDeptId={}, ihrDeptUuid={}, reason=mdm_dept_src_ref 未命中",
                    BIZ, ihrDeptIdStr, odsDept.getIhrDeptUuid());
            return null;
        }
        return deptRef.getDeptId();
    }

    /**
     * List&lt;Integer&gt;（positionScope）→ JSON 字符串（PG JSONB 列）
     *
     * <p>null / 空列表返回 null；rowHash 与 ODS 落库共用本方法输出，保证跨接口稳定。</p>
     *
     * @param scope 应用范围部门 ID 列表（可为 null）
     * @return JSON 字符串；null 或空列表时返回 null
     */
    private String toJson(List<Integer> scope) {
        if (CollectionUtils.isEmpty(scope)) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(scope);
        } catch (Exception e) {
            log.info("【{}】positionScope JSON 序列化终止, reason={}", BIZ, e.getMessage());
            return null;
        }
    }
}
