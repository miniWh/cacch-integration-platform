package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微 — 创建预约会议请求
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComCreateMeetingRequest {

    @JsonProperty("admin_userid")
    private String adminUserid;

    private String title;

    @JsonProperty("meeting_start")
    private Long meetingStart;

    @JsonProperty("meeting_duration")
    private Integer meetingDuration;

    private String description;

    private String location;

    private Integer agentid;

    private WeComMeetingInvitees invitees;

    @Data
    @Builder
    public static class WeComMeetingInvitees {

        private List<String> userid;
    }
}
