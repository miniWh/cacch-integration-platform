package com.cacch.integration.common.config.meeting;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 会议同步相关配置属性 — 由 yml 的 meeting.sync 绑定
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@ConfigurationProperties(prefix = "meeting.sync")
public class MeetingSyncProperties {

    /**
     * 会议结束后再等待多少分钟才开始拉取纪要，默认 5
     */
    private int minutesEndBufferMinutes = 5;

    /**
     * 纪要最大等待小时数，超时后标记为未获取，默认 48
     */
    private int minutesMaxWaitHours = 48;
}
