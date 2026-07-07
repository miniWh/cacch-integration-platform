package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企微 — 获取会议详情响应（仅映射业务所需字段）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComGetMeetingInfoResponse extends WeComBaseResponse {

    private String meetingid;

    @JsonProperty("meeting_code")
    private String meetingCode;

    @JsonProperty("meeting_link")
    private String meetingLink;

    private String title;

    private String description;

    private String location;

    @JsonProperty("meeting_start")
    private Long meetingStart;

    @JsonProperty("meeting_end")
    private Long meetingEnd;

    @JsonProperty("meeting_duration")
    private Integer meetingDuration;

    private Integer status;

    private WeComMeetingAttendeesInfo attendees;
}
