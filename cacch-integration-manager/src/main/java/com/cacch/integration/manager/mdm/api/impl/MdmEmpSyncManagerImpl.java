package com.cacch.integration.manager.mdm.api.impl;

import com.cacch.integration.common.constant.mdm.MdmCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicResponse;
import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager;
import com.cacch.integration.manager.mdm.emp.incremental.api.IMdmEmpIncrementalProcessPhase;
import com.cacch.integration.manager.mdm.emp.incremental.api.IMdmEmpIncrementalPullPhase;
import com.cacch.integration.manager.mdm.emp.incremental.api.IMdmEmpIncrementalSyncUseCase;
import com.cacch.integration.manager.mdm.emp.incremental.model.IncrementalPullPhaseResult;
import com.cacch.integration.manager.mdm.emp.incremental.model.PullRunMode;
import com.cacch.integration.manager.mdm.emp.incremental.support.MdmEmpIngestPrepareCoordinator;
import com.cacch.integration.manager.mdm.emp.incremental.support.MdmEmpIngestResultRecorder;
import com.cacch.integration.manager.mdm.emp.incremental.support.MdmEmpSyncExecutionGuard;
import com.cacch.integration.service.ihr.api.IIhrStaffService;
import com.cacch.integration.service.mdm.api.IMdmEmpIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 人员主数据（MDM）iHR 人员同步 Manager —— 对外 Facade（全量 + 增量委托）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmEmpSyncManagerImpl implements IMdmEmpSyncManager {

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;

    private final MdmEmpSyncExecutionGuard executionGuard;
    private final IMdmEmpIncrementalSyncUseCase incrementalSyncUseCase;
    private final IMdmEmpIncrementalPullPhase pullPhase;
    private final IMdmEmpIncrementalProcessPhase processPhase;
    private final MdmEmpIngestPrepareCoordinator ingestPrepareCoordinator;
    private final IIhrStaffService ihrStaffService;
    private final IMdmEmpIngestService mdmEmpIngestService;

    @Override
    public BatchProcessResult<String> pullAndIngest() {
        log.info("【{}】全量触发", BIZ);
        return executionGuard.executeFullSync(this::doPullAndIngest);
    }

    @Override
    public PullIncrementalResult pullIncrementalToQueue() {
        IncrementalPullPhaseResult outcome = pullPhase.run(PullRunMode.FAIL_FAST);
        return outcome.pullResult();
    }

    @Override
    public BatchProcessResult<String> processIncrementalFromQueue(int limit) {
        return processPhase.run(limit).batchResult();
    }

    @Override
    public IncrementalSyncResult runIncrementalSync(int limit) {
        return incrementalSyncUseCase.run(limit);
    }

    private BatchProcessResult<String> doPullAndIngest() {
        String etlBatchNo = MdmCommonConstants.buildEtlBatchNo("IHR");
        log.info("【{}】开始本轮 iHR 人员拉取落库, batchNo={}", BIZ, etlBatchNo);

        List<String> staffIds = ihrStaffService.fetchAllStaffIds(null);
        BatchProcessResult<String> result = new BatchProcessResult<>();
        if (CollectionUtils.isEmpty(staffIds)) {
            log.info("【{}】员工 ID 清单为空, 本轮结束, batchNo={}", BIZ, etlBatchNo);
            return result;
        }

        Map<String, IhrStaffBasicResponse> responses = new LinkedHashMap<>();
        for (String staffId : staffIds) {
            try {
                responses.put(staffId, ihrStaffService.fetchStaffBasicResponse(staffId));
            } catch (Exception e) {
                log.info("【{}】单员工拉取终止, ihrStaffId={}, batchNo={}, reason={}",
                        BIZ, staffId, etlBatchNo, e.getMessage());
                log.error("【{}】单员工拉取失败, ihrStaffId={}", BIZ, staffId, e);
                result.recordFailure(staffId, e.getMessage());
            }
        }

        ingestPrepareCoordinator.ensureDepartmentsBeforeIngest(responses, etlBatchNo);
        ingestPrepareCoordinator.ensurePositionsBeforeIngest(responses, etlBatchNo);

        for (Map.Entry<String, IhrStaffBasicResponse> entry : responses.entrySet()) {
            String staffId = entry.getKey();
            try {
                IhrStaffBasicResponse resp = entry.getValue();
                if (resp.getData() == null) {
                    log.info("【{}】响应无 data, 跳过该员工, ihrStaffId={}, batchNo={}", BIZ, staffId, etlBatchNo);
                    result.recordSkip(staffId, "响应无 data");
                    continue;
                }
                MdmEmpIngestResultRecorder.record(result, staffId,
                        mdmEmpIngestService.ingestOne(resp.getData(), resp.getRawJson(), etlBatchNo));
            } catch (Exception e) {
                log.info("【{}】单员工处理终止, ihrStaffId={}, batchNo={}, reason={}",
                        BIZ, staffId, etlBatchNo, e.getMessage());
                log.error("【{}】单员工落库失败, ihrStaffId={}", BIZ, staffId, e);
                result.recordFailure(staffId, e.getMessage());
            }
        }

        log.info("【{}】本轮拉取落库完成, batchNo={}, 总数={}, 成功={}, 跳过={}, 失败={}",
                BIZ, etlBatchNo, result.getTotalCount(), result.getSuccessCount(),
                result.getSkipCount(), result.getFailureCount());
        return result;
    }
}