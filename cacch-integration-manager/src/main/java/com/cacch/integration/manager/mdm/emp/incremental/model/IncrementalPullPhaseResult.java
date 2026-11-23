package com.cacch.integration.manager.mdm.emp.incremental.model;

import com.cacch.integration.common.enums.mdm.MdmEmpIncrementalSyncPhaseStatus;
import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager.PullIncrementalResult;

/**
 * 增量 Pull 阶段执行结果
 *
 * @author hongfu_zhou@cacch.com
 */
public record IncrementalPullPhaseResult(
        MdmEmpIncrementalSyncPhaseStatus status,
        PullIncrementalResult pullResult,
        String errorMessage
) {

    /**
     * Pull 成功
     *
     * @param pullResult 入队统计
     * @return 阶段结果
     */
    public static IncrementalPullPhaseResult success(PullIncrementalResult pullResult) {
        return new IncrementalPullPhaseResult(MdmEmpIncrementalSyncPhaseStatus.SUCCESS, pullResult, null);
    }

    /**
     * Pull 业务/集成失败
     *
     * @param errorMessage 失败原因
     * @return 阶段结果
     */
    public static IncrementalPullPhaseResult failed(String errorMessage) {
        return new IncrementalPullPhaseResult(MdmEmpIncrementalSyncPhaseStatus.FAILED, null, errorMessage);
    }

    /**
     * Pull 互斥拒绝
     *
     * @param errorMessage 拒绝原因
     * @return 阶段结果
     */
    public static IncrementalPullPhaseResult rejectedMutex(String errorMessage) {
        return new IncrementalPullPhaseResult(MdmEmpIncrementalSyncPhaseStatus.REJECTED_MUTEX, null, errorMessage);
    }
}