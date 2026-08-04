package com.cacch.integration.config.fdd;

import com.cacch.integration.common.config.fdd.FddProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 法大大配置注册
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(FddProperties.class)
public class FddConfiguration {
}
