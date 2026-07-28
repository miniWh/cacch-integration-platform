package com.cacch.integration.config.sharedrive;

import com.cacch.integration.common.config.sharedrive.ShareDriveProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 共享盘配置注册
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(ShareDriveProperties.class)
public class ShareDriveConfiguration {
}
