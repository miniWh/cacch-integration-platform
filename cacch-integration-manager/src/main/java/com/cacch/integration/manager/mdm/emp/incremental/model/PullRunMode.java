package com.cacch.integration.manager.mdm.emp.incremental.model;

/**
 * 增量 Pull 阶段运行模式
 *
 * @author hongfu_zhou@cacch.com
 */
public enum PullRunMode {

    /**
     * 失败即抛异常（运维单独 Pull 入口）
     */
    FAIL_FAST,

    /**
     * 失败写入阶段结果，不抛异常（ESB 编排内 Pull）
     */
    SWALLOW_FOR_ORCHESTRATION
}