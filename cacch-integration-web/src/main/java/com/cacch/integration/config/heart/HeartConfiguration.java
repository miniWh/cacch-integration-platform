package com.cacch.integration.config.heart;

import com.cacch.integration.common.config.heart.HeartProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Heart 订单自动化配置注册
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(HeartProperties.class)
public class HeartConfiguration {
}
