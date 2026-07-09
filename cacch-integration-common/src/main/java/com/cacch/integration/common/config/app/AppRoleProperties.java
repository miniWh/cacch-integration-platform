package com.cacch.integration.common.config.app;

import com.cacch.integration.common.constant.app.AppRoleConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 应用运行角色配置 — 由 yml 的 app.role 绑定
 *
 * <pre>
 * app.role=api       → 仅 HTTP 接口，不启动 @Scheduled
 * app.role=all       → HTTP + 定时任务（正式单实例推荐）
 * app.role=scheduler → 与 all 相同，开启定时任务
 * </pre>
 *
 * @author hongfu_zhou@cacch.com
 */
@ConfigurationProperties(prefix = "app")
public class AppRoleProperties {

    /**
     * 运行角色：api / all / scheduler，默认 api
     */
    private final String role;

    public AppRoleProperties(String role) {
        this.role = StringUtils.hasText(role) ? role.trim() : AppRoleConstants.ROLE_API;
    }

    /**
     * @return 当前运行角色（小写归一前的原始配置值）
     */
    public String getRole() {
        return role;
    }

    /**
     * 是否应启动 Spring 定时调度
     *
     * @return true 表示 role 为 all 或 scheduler
     */
    public boolean isSchedulingEnabled() {
        String normalized = role.toLowerCase();
        return AppRoleConstants.ROLE_ALL.equals(normalized)
                || AppRoleConstants.ROLE_SCHEDULER.equals(normalized);
    }

    /**
     * 是否为仅 API 角色
     *
     * @return true 表示不启动定时任务
     */
    public boolean isApiOnly() {
        return !isSchedulingEnabled();
    }
}
