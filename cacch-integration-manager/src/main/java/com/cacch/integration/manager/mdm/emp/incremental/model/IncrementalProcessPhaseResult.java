package com.cacch.integration.manager.mdm.emp.incremental.model;

import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.enums.mdm.MdmEmpIncrementalSyncPhaseStatus;

/**
 * 增量 Process 阶段执行结果
 *
 * @author hongfu_zhou@cacch.com
 */
public record IncrementalProcessPhaseResult(
        MdmEmpIncrementalSyncPhaseStatus status,
        BatchProcessResult<String> batchResult,
        String errorMessage
) {

    /**
     * Process 成功且无单条失败
     *
     * @param batchResult 落库统计
     * @return 阶段结果
     */
    public static IncrementalProcessPhaseResult success(BatchProcessResult<String> batchResult) {
        return new IncrementalProcessPhaseResult(MdmEmpIncrementalSyncPhaseStatus.SUCCESS, batchResult, null);
    }

    /**
     * Process 完成但存在单员工失败
     *
     * @param batchResult 落库统计（含失败明细）
     * @return 阶段结果
     */
    public static IncrementalProcessPhaseResult partialFailure(BatchProcessResult<String> batchResult) {
        return new IncrementalProcessPhaseResult(
                MdmEmpIncrementalSyncPhaseStatus.PARTIAL_FAILURE, batchResult, null);
    }
}