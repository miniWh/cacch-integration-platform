package com.cacch.integration.config.wecom;

import com.cacch.integration.common.config.wecom.WeComProperties;
import com.cacch.integration.common.config.wecom.WeComWebhookProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 企业微信配置注册 — 在 web 启动层完成 yml 绑定，供 manager/async 等模块注入
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties({WeComProperties.class, WeComWebhookProperties.class})
public class WeComConfiguration {
}
