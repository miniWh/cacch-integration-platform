package com.cacch.integration.manager.mdm.emp.incremental.api;

import com.cacch.integration.manager.mdm.emp.incremental.model.IncrementalProcessPhaseResult;

/**
 * 人员增量 Process 阶段 —— 队列认领与落库
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMdmEmpIncrementalProcessPhase {

    /**
     * 执行 Process 阶段
     *
     * @param limit 单次认领上限
     * @return 阶段结果；整轮失败时抛异常
     * @throws com.cacch.integration.common.exception.BizException 互斥拒绝或整轮未捕获异常
     */
    IncrementalProcessPhaseResult run(int limit);
}