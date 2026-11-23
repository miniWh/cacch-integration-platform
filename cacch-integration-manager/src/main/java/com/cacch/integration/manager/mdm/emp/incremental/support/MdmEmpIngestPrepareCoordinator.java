package com.cacch.integration.manager.mdm.emp.incremental.support;

import com.cacch.integration.common.constant.mdm.MdmCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.enums.mdm.MdmIngestResultEnum;
import com.cacch.integration.entity.mdm.StgStaffRecordDO;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.integration.ihr.client.dto.IhrPosition;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicResponse;
import com.cacch.integration.service.ihr.api.IIhrOrgService;
import com.cacch.integration.service.ihr.api.IIhrPositionSyncService;
import com.cacch.integration.service.ihr.api.IIhrStaffService;
import com.cacch.integration.service.mdm.api.IMdmDeptIngestService;
import com.cacch.integration.service.mdm.api.IMdmEmpIncrementalQueueService;
import com.cacch.integration.service.mdm.api.IMdmEmpIngestService;
import com.cacch.integration.service.mdm.api.IMdmPositionIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 员工落库前准备 —— 拉取员工详情、部门/岗位保障（全量与增量 Process 共用）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MdmEmpIngestPrepareCoordinator {

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final IIhrStaffService ihrStaffService;
    private final IMdmEmpIngestService mdmEmpIngestService;
    private final IMdmEmpIncrementalQueueService incrementalQueueService;
    private final IIhrOrgService ihrOrgService;
    private final IMdmDeptIngestService mdmDeptIngestService;
    private final IIhrPositionSyncService ihrPositionSyncService;
    private final IMdmPositionIngestService mdmPositionIngestService;

    /**
     * 增量 Process：为认领批次拉取员工详情（失败行记失败并 mark PROCESSED）
     *
     * @param batch           队列行
     * @param processBatchNo  处理批次号
     * @param aggregateResult 累计统计
     * @return staffId → 员工详情（仅成功拉取）
     */
    public Map<String, IhrStaffBasicResponse> fetchStaffBasicsForQueueBatch(
            List<StgStaffRecordDO> batch,
            String processBatchNo,
            BatchProcessResult<String> aggregateResult) {
        Map<String, IhrStaffBasicResponse> responses = new LinkedHashMap<>();
        for (StgStaffRecordDO row : batch) {
            try {
                responses.put(row.getStaffId(), ihrStaffService.fetchStaffBasicResponse(row.getStaffId()));
            } catch (Exception e) {
                log.info("【{}】单员工拉取终止, staffId={}, processBatchNo={}, reason={}",
                        BIZ, row.getStaffId(), processBatchNo, e.getMessage());
                log.error("【{}】单员工拉取失败, staffId={}", BIZ, row.getStaffId(), e);
                aggregateResult.recordFailure(row.getStaffId(), e.getMessage());
                incrementalQueueService.markOneProcessed(row.getId(), processBatchNo);
            }
        }
        return responses;
    }

    /**
     * 部门保障（变更前 + 变更后）
     *
     * @param responses     员工详情 Map
     * @param empEtlBatchNo 员工 ETL 批次号
     */
    public void ensureDepartmentsBeforeIngest(Map<String, IhrStaffBasicResponse> responses, String empEtlBatchNo) {
        if (responses.isEmpty()) {
            log.info("【{}】部门保障跳过, reason=本轮无员工响应, empBatchNo={}", BIZ, empEtlBatchNo);
            return;
        }

        Set<Long> deptIds = new LinkedHashSet<>();
        for (IhrStaffBasicResponse resp : responses.values()) {
            if (resp.getData() != null && resp.getData().getDepartmentId() != null) {
                deptIds.add(resp.getData().getDepartmentId());
            }
        }
        Map<String, String> prevDeptIds = mdmEmpIngestService.findPrevDeptIdByStaffIds(responses.keySet());
        for (String deptId : prevDeptIds.values()) {
            Long parsed = parseDeptIdSafe(deptId);
            if (parsed != null) {
                deptIds.add(parsed);
            }
        }
        if (deptIds.isEmpty()) {
            log.info("【{}】部门保障跳过, reason=本轮涉及部门为空, empBatchNo={}", BIZ, empEtlBatchNo);
            return;
        }

        String deptEtlBatchNo = MdmCommonConstants.buildEtlBatchNo("DEPT");
        log.info("【{}】员工落库前部门保障开始, 涉及部门数={}, empBatchNo={}, deptBatchNo={}",
                BIZ, deptIds.size(), empEtlBatchNo, deptEtlBatchNo);
        try {
            List<IhrDepartment> depts = ihrOrgService.listDepartmentsByIds(new ArrayList<>(deptIds));
            if (CollectionUtils.isEmpty(depts)) {
                log.info("【{}】部门保障终止, reason=iHR 未返回任何部门, deptBatchNo={}", BIZ, deptEtlBatchNo);
                return;
            }
            BatchProcessResult<String> deptResult = new BatchProcessResult<>();
            for (IhrDepartment dept : depts) {
                try {
                    String rawJson = MAPPER.writeValueAsString(dept);
                    MdmIngestResultEnum ingestResult =
                            mdmDeptIngestService.ingestOne(dept, rawJson, deptEtlBatchNo);
                    if (ingestResult == MdmIngestResultEnum.INGESTED) {
                        deptResult.recordSuccess(dept.getUuid());
                    } else {
                        deptResult.recordSkip(dept.getUuid(), ingestResult.getDesc());
                    }
                } catch (Exception e) {
                    log.info("【{}】部门保障单部门处理终止, ihrDeptUuid={}, deptBatchNo={}, reason={}",
                            BIZ, dept.getUuid(), deptEtlBatchNo, e.getMessage());
                    log.error("【{}】部门保障单部门落库失败, ihrDeptUuid={}", BIZ, dept.getUuid(), e);
                    deptResult.recordFailure(dept.getUuid(), e.getMessage());
                }
            }
            log.info("【{}】员工落库前部门保障完成, 涉及部门数={}, iHR返回={}, 已落库={}, 无变化跳过={}, 失败={}, deptBatchNo={}",
                    BIZ, deptIds.size(), depts.size(), deptResult.getSuccessCount(),
                    deptResult.getSkipCount(), deptResult.getFailureCount(), deptEtlBatchNo);
        } catch (Exception e) {
            log.info("【{}】员工落库前部门保障终止, 员工落库继续, empBatchNo={}, reason={}",
                    BIZ, empEtlBatchNo, e.getMessage());
            log.error("【{}】员工落库前部门保障失败, empBatchNo={}", BIZ, empEtlBatchNo, e);
        }
    }

    /**
     * 岗位保障（变更前 + 变更后）
     *
     * @param responses     员工详情 Map
     * @param empEtlBatchNo 员工 ETL 批次号
     */
    public void ensurePositionsBeforeIngest(Map<String, IhrStaffBasicResponse> responses, String empEtlBatchNo) {
        if (responses.isEmpty()) {
            log.info("【{}】岗位保障跳过, reason=本轮无员工响应, empBatchNo={}", BIZ, empEtlBatchNo);
            return;
        }

        Set<String> positionIds = new LinkedHashSet<>();
        for (IhrStaffBasicResponse resp : responses.values()) {
            if (resp.getData() != null && StringUtils.hasText(resp.getData().getPositionId())) {
                positionIds.add(resp.getData().getPositionId());
            }
        }
        Map<String, String> prevPositionIds = mdmEmpIngestService.findPrevPositionIdByStaffIds(responses.keySet());
        for (String positionId : prevPositionIds.values()) {
            if (StringUtils.hasText(positionId)) {
                positionIds.add(positionId);
            }
        }
        if (positionIds.isEmpty()) {
            log.info("【{}】岗位保障跳过, reason=本轮涉及岗位为空, empBatchNo={}", BIZ, empEtlBatchNo);
            return;
        }

        String positionEtlBatchNo = MdmCommonConstants.buildEtlBatchNo("POS");
        log.info("【{}】员工落库前岗位保障开始, 涉及岗位数={}, empBatchNo={}, positionBatchNo={}",
                BIZ, positionIds.size(), empEtlBatchNo, positionEtlBatchNo);
        try {
            List<IhrPosition> positions = ihrPositionSyncService.listPositionsByIds(new ArrayList<>(positionIds));
            if (CollectionUtils.isEmpty(positions)) {
                log.info("【{}】岗位保障终止, reason=iHR 未返回任何岗位, positionBatchNo={}", BIZ, positionEtlBatchNo);
                return;
            }
            BatchProcessResult<String> positionResult = new BatchProcessResult<>();
            for (IhrPosition position : positions) {
                try {
                    String rawJson = MAPPER.writeValueAsString(position);
                    MdmIngestResultEnum ingestResult =
                            mdmPositionIngestService.ingestOne(position, rawJson, positionEtlBatchNo);
                    if (ingestResult == MdmIngestResultEnum.INGESTED) {
                        positionResult.recordSuccess(position.getId());
                    } else {
                        positionResult.recordSkip(position.getId(), ingestResult.getDesc());
                    }
                } catch (Exception e) {
                    log.info("【{}】岗位保障单岗位处理终止, ihrPositionUuid={}, positionBatchNo={}, reason={}",
                            BIZ, position.getId(), positionEtlBatchNo, e.getMessage());
                    log.error("【{}】岗位保障单岗位落库失败, ihrPositionUuid={}", BIZ, position.getId(), e);
                    positionResult.recordFailure(position.getId(), e.getMessage());
                }
            }
            log.info("【{}】员工落库前岗位保障完成, 涉及岗位数={}, iHR返回={}, 已落库={}, 无变化跳过={}, 失败={}, positionBatchNo={}",
                    BIZ, positionIds.size(), positions.size(), positionResult.getSuccessCount(),
                    positionResult.getSkipCount(), positionResult.getFailureCount(), positionEtlBatchNo);
        } catch (Exception e) {
            log.info("【{}】员工落库前岗位保障终止, 员工落库继续, empBatchNo={}, reason={}",
                    BIZ, empEtlBatchNo, e.getMessage());
            log.error("【{}】员工落库前岗位保障失败, empBatchNo={}", BIZ, empEtlBatchNo, e);
        }
    }

    private Long parseDeptIdSafe(String deptId) {
        try {
            return Long.parseLong(deptId);
        } catch (NumberFormatException e) {
            log.info("【{}】上一快照部门ID非数字, 跳过该部门保障, ihrDeptId={}", BIZ, deptId);
            return null;
        }
    }
}