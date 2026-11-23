package com.cacch.integration.common.enums.mdm;

/**
 * 人员增量同步单阶段执行状态（Pull / Process 共用）
 *
 * @author hongfu_zhou@cacch.com
 */
public enum MdmEmpIncrementalSyncPhaseStatus {

    /**
     * 阶段按预期完成（Process 允许存在单条失败，见 {@link #PARTIAL_FAILURE}）
     */
    SUCCESS,

    /**
     * Process 阶段完成，但存在单员工拉取或落库失败
     */
    PARTIAL_FAILURE,

    /**
     * 阶段因业务/集成异常终止（如 iHR 调用失败）
     */
    FAILED,

    /**
     * 同阶段上一轮仍在执行，互斥拒绝
     */
    REJECTED_MUTEX
}