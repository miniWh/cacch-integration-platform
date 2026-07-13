package com.cacch.integration.config.oa;

import com.cacch.integration.common.config.oa.OaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 致远 OA 配置注册
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(OaProperties.class)
public class OaConfiguration {
}
