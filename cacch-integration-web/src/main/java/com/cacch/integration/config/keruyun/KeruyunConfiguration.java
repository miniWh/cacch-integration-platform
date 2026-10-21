package com.cacch.integration.config.keruyun;

import com.cacch.integration.common.config.keruyun.KeruyunProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 客如云配置注册
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(KeruyunProperties.class)
public class KeruyunConfiguration {
}
