package com.cacch.integration.manager.mdm.emp.incremental.phase;

import com.cacch.integration.common.constant.mdm.MdmCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.mdm.StgStaffRecordDO;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicResponse;
import com.cacch.integration.manager.mdm.emp.incremental.api.IMdmEmpIncrementalProcessPhase;
import com.cacch.integration.manager.mdm.emp.incremental.model.IncrementalProcessPhaseResult;
import com.cacch.integration.manager.mdm.emp.incremental.support.MdmEmpIngestPrepareCoordinator;
import com.cacch.integration.manager.mdm.emp.incremental.support.MdmEmpIngestResultRecorder;
import com.cacch.integration.manager.mdm.emp.incremental.support.MdmEmpIncrementalSyncAlerter;
import com.cacch.integration.manager.mdm.emp.incremental.support.MdmEmpSyncExecutionGuard;
import com.cacch.integration.service.mdm.api.IMdmEmpIncrementalQueueService;
import com.cacch.integration.service.mdm.api.IMdmEmpIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 人员增量 Process 阶段实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmEmpIncrementalProcessPhaseImpl implements IMdmEmpIncrementalProcessPhase {

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;
    private static final String MUTEX_REJECT_REASON = "上一轮增量 Process 仍在执行中，请勿重复触发";

    private final MdmEmpSyncExecutionGuard executionGuard;
    private final MdmEmpIncrementalSyncAlerter alerter;
    private final MdmEmpIngestPrepareCoordinator ingestPrepareCoordinator;
    private final IMdmEmpIncrementalQueueService incrementalQueueService;
    private final IMdmEmpIngestService mdmEmpIngestService;

    @Override
    public IncrementalProcessPhaseResult run(int limit) {
        if (!executionGuard.tryAcquireProcess()) {
            log.info("【{}】Process增量触发被拒绝, reason={}", BIZ, MUTEX_REJECT_REASON);
            alerter.alertProcessStage(MUTEX_REJECT_REASON, null, limit);
            throw new BizException(ResultCode.SYSTEM_ERROR, MUTEX_REJECT_REASON);
        }
        try {
            BatchProcessResult<String> result = executeProcessBody(limit);
            if (result.hasFailure()) {
                alerter.alertProcessPartialFailure(result, limit);
                return IncrementalProcessPhaseResult.partialFailure(result);
            }
            return IncrementalProcessPhaseResult.success(result);
        } catch (Exception e) {
            alerter.alertProcessStage(e.getMessage(), e, limit);
            throw e;
        } finally {
            executionGuard.releaseProcess();
        }
    }

    private BatchProcessResult<String> executeProcessBody(int limit) {
        String processBatchNo = MdmCommonConstants.buildProcessBatchNo();
        String etlBatchNo = MdmCommonConstants.buildEtlBatchNo("IHR");
        log.info("【{}】开始本轮增量 Process, processBatchNo={}, etlBatchNo={}, limit={}",
                BIZ, processBatchNo, etlBatchNo, limit);

        List<StgStaffRecordDO> batch = incrementalQueueService.claimPendingBatch(limit);
        if (CollectionUtils.isEmpty(batch)) {
            log.info("【{}】PENDING 队列为空, 本轮 Process 结束", BIZ);
            return new BatchProcessResult<>();
        }
        log.info("【{}】已认领 {} 行待处理（ADD先 / UPDATE按lastUpdate升序）", BIZ, batch.size());

        BatchProcessResult<String> result = new BatchProcessResult<>();
        Map<String, IhrStaffBasicResponse> responses =
                ingestPrepareCoordinator.fetchStaffBasicsForQueueBatch(batch, processBatchNo, result);

        ingestPrepareCoordinator.ensureDepartmentsBeforeIngest(responses, etlBatchNo);
        ingestPrepareCoordinator.ensurePositionsBeforeIngest(responses, etlBatchNo);

        ingestAndMarkQueueRows(batch, responses, processBatchNo, etlBatchNo, result);

        log.info("【{}】本轮增量 Process 完成, processBatchNo={}, 总数={}, 成功={}, 跳过={}, 失败={}",
                BIZ, processBatchNo, result.getTotalCount(), result.getSuccessCount(),
                result.getSkipCount(), result.getFailureCount());
        return result;
    }

    private void ingestAndMarkQueueRows(List<StgStaffRecordDO> batch,
                                        Map<String, IhrStaffBasicResponse> responses,
                                        String processBatchNo,
                                        String etlBatchNo,
                                        BatchProcessResult<String> result) {
        for (StgStaffRecordDO row : batch) {
            if (!responses.containsKey(row.getStaffId())) {
                continue;
            }
            try {
                IhrStaffBasicResponse resp = responses.get(row.getStaffId());
                if (resp.getData() == null) {
                    log.info("【{}】响应无 data, 跳过该行, staffId={}, processBatchNo={}",
                            BIZ, row.getStaffId(), processBatchNo);
                    result.recordSkip(row.getStaffId(), "iHR 响应无 data");
                    continue;
                }
                MdmEmpIngestResultRecorder.record(result, row.getStaffId(),
                        mdmEmpIngestService.ingestOne(resp.getData(), resp.getRawJson(), etlBatchNo));
            } catch (Exception e) {
                log.info("【{}】单员工处理终止, staffId={}, processBatchNo={}, reason={}",
                        BIZ, row.getStaffId(), processBatchNo, e.getMessage());
                log.error("【{}】单员工增量落库失败, staffId={}", BIZ, row.getStaffId(), e);
                result.recordFailure(row.getStaffId(), e.getMessage());
            } finally {
                incrementalQueueService.markOneProcessed(row.getId(), processBatchNo);
            }
        }
    }
}