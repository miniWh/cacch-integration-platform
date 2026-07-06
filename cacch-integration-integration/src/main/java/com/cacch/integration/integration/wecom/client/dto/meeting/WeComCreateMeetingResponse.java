package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 企微 — 创建预约会议响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComCreateMeetingResponse extends WeComBaseResponse {

    private String meetingid;

    @JsonProperty("excess_users")
    private List<String> excessUsers;
}
