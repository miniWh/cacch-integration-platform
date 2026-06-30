package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微 — 获取会议详情请求
 */
@Data
@Builder
public class WeComGetMeetingInfoRequest {

    private String meetingid;
}
