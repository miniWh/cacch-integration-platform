package com.cacch.integration.integration.tencentmeeting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 腾讯会议智能纪要 API 响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TencentMeetingSmartMinutesResponse {

    @JsonProperty("meeting_minute")
    private MeetingMinute meetingMinute;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MeetingMinute {

        private String minute;

        private String todo;
    }
}
