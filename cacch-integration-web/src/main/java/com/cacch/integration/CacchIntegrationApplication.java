package com.cacch.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Cacch 集成平台启动类
 *
 * <p>定时调度由 {@code app.role} 控制：仅 {@code all}/{@code scheduler} 时启用，
 * 见 {@link com.cacch.integration.config.app.AppRoleConfiguration}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@SpringBootApplication
public class CacchIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacchIntegrationApplication.class, args);
    }
}
