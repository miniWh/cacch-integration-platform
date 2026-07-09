package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微 — 创建预约会议请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComCreateMeetingRequest {

    /**
     * 会议管理员 userid（企微侧会议管理身份，对应业务上的会议创建人）
     */
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

    private WeComMeetingSettings settings;

    @Data
    @Builder
    public static class WeComMeetingInvitees {

        private List<String> userid;
    }

    @Data
    @Builder
    public static class WeComMeetingSettings {

        /**
         * 会议主持人列表；若含 admin_userid，企微会自动过滤
         */
        private WeComMeetingHosts hosts;
    }

    @Data
    @Builder
    public static class WeComMeetingHosts {

        private List<String> userid;
    }
}
