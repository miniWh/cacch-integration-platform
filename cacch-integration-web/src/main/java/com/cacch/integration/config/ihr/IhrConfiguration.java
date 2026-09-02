package com.cacch.integration.config.ihr;

import com.cacch.integration.common.config.ihr.IhrProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * IHR 开放平台配置注册 — 在 web 启动层完成 yml 绑定，供 manager/service 模块注入
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(IhrProperties.class)
public class IhrConfiguration {
}
