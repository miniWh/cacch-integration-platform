package com.cacch.integration.integration.wecom.client.dto.meeting;

import lombok.Builder;
import lombok.Data;

/**
 * 企微 — 获取会议详情请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComGetMeetingInfoRequest {

    private String meetingid;
}
