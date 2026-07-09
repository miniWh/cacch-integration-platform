package com.cacch.integration.config.app;

import com.cacch.integration.common.config.app.AppRoleProperties;
import com.cacch.integration.common.constant.app.AppRoleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

/**
 * 应用角色与定时调度注册
 *
 * <p>仅当 {@code app.role=all} 或 {@code app.role=scheduler} 时启用 {@code @EnableScheduling}；
 * {@code app.role=api} 时不注册调度，HTTP 接口与手动同步 API 仍可用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(AppRoleProperties.class)
public class AppRoleConfiguration {

    /**
     * role=all 时启用定时调度
     */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "app", name = "role", havingValue = AppRoleConstants.ROLE_ALL)
    static class SchedulingOnAllRoleConfiguration {
    }

    /**
     * role=scheduler 时启用定时调度（与 all 等价）
     */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "app", name = "role", havingValue = AppRoleConstants.ROLE_SCHEDULER)
    static class SchedulingOnSchedulerRoleConfiguration {
    }

    /**
     * 启动时打印当前应用角色，便于确认调度是否开启
     */
    @Slf4j
    @Component
    @Order(0)
    static class AppRoleStartupLogger implements ApplicationRunner {

        private final AppRoleProperties appRoleProperties;

        AppRoleStartupLogger(AppRoleProperties appRoleProperties) {
            this.appRoleProperties = appRoleProperties;
        }

        /**
         * 应用启动完成后输出角色与调度开关状态
         *
         * @param args 启动参数，可忽略
         */
        @Override
        public void run(ApplicationArguments args) {
            log.info("【AppRole】当前角色={}, schedulingEnabled={}（api=仅接口；all/scheduler=接口+定时任务）",
                    appRoleProperties.getRole(), appRoleProperties.isSchedulingEnabled());
        }
    }
}
