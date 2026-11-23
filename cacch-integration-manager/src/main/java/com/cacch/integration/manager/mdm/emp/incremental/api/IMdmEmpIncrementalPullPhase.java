package com.cacch.integration.manager.mdm.emp.incremental.api;

import com.cacch.integration.manager.mdm.emp.incremental.model.IncrementalPullPhaseResult;
import com.cacch.integration.manager.mdm.emp.incremental.model.PullRunMode;

/**
 * 人员增量 Pull 阶段 —— iHR 清单入队 stg_staff_record
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMdmEmpIncrementalPullPhase {

    /**
     * 执行 Pull 阶段
     *
     * @param mode {@link PullRunMode#FAIL_FAST} 时失败抛异常；{@link PullRunMode#SWALLOW_FOR_ORCHESTRATION} 时写入结果
     * @return 阶段结果（FAIL_FAST 且失败时可能不返回，直接抛异常）
     * @throws com.cacch.integration.common.exception.BizException FAIL_FAST 且阶段失败时抛出
     */
    IncrementalPullPhaseResult run(PullRunMode mode);
}