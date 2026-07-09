package com.cacch.integration.config.meeting;

import com.cacch.integration.common.config.meeting.MeetingSyncProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 会议同步配置注册 — 在 web 启动层完成 yml 绑定
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(MeetingSyncProperties.class)
public class MeetingConfiguration {
}
