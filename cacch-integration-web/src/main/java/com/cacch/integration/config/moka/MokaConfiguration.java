package com.cacch.integration.config.moka;

import com.cacch.integration.common.config.moka.MokaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Moka 开放平台配置注册 —— 将 {@link MokaProperties} 注册为 Spring Bean
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(MokaProperties.class)
public class MokaConfiguration {
}
