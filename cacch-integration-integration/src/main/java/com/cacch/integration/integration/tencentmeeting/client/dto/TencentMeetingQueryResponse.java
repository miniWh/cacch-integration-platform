package com.cacch.integration.integration.tencentmeeting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 腾讯会议查询响应（/v1/meetings）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TencentMeetingQueryResponse {

    @JsonProperty("meeting_number")
    private Integer meetingNumber;

    @JsonProperty("meeting_info_list")
    private List<MeetingInfo> meetingInfoList;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MeetingInfo {

        @JsonProperty("meeting_id")
        private String meetingId;

        @JsonProperty("meeting_code")
        private String meetingCode;

        private String subject;

        private String status;

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("end_time")
        private String endTime;
    }
}
