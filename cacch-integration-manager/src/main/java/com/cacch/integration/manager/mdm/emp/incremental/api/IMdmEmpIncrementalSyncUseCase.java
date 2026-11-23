package com.cacch.integration.manager.mdm.emp.incremental.api;

import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager.IncrementalSyncResult;

/**
 * 人员增量同步用例 —— ESB 编排入口（Pull → Process）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMdmEmpIncrementalSyncUseCase {

    /**
     * 执行一轮增量同步：Pull 失败不阻断 Process
     *
     * @param limit Process 单次认领上限，有效范围 1～1000
     * @return 两阶段状态与统计
     * @throws com.cacch.integration.common.exception.BizException Process 互斥拒绝或整轮异常时抛出
     */
    IncrementalSyncResult run(int limit);
}