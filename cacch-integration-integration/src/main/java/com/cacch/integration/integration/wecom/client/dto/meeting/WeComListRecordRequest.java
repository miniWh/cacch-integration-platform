package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微 — 获取会议录制列表请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComListRecordRequest {

    private String meetingid;

    @JsonProperty("meeting_code")
    private String meetingCode;

    private String userid;

    @JsonProperty("start_time")
    private Long startTime;

    @JsonProperty("end_time")
    private Long endTime;

    private String cursor;

    private Integer limit;
}
