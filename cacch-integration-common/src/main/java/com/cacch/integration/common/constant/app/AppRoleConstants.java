package com.cacch.integration.common.constant.app;

/**
 * 应用运行角色常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class AppRoleConstants {

    private AppRoleConstants() {
    }

    /**
     * 仅提供 HTTP API，不启动定时任务（测试环境默认）
     */
    public static final String ROLE_API = "api";

    /**
     * 提供 HTTP API 并启动定时任务（正式环境默认，单进程同时承担接口与调度）
     */
    public static final String ROLE_ALL = "all";

    /**
     * 与 {@link #ROLE_ALL} 等价：开启调度（兼容「scheduler」命名）
     */
    public static final String ROLE_SCHEDULER = "scheduler";
}
