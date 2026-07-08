package com.cacch.integration.config.tencentmeeting;

import com.cacch.integration.common.config.tencentmeeting.TencentMeetingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯会议配置注册
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(TencentMeetingProperties.class)
public class TencentMeetingConfiguration {
}
