package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 企微 — 获取会议录制列表响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComListRecordResponse extends WeComBaseResponse {

    @JsonProperty("record_list")
    @JsonAlias("record_meetings")
    private List<WeComRecordMeetingInfo> recordList;

    @JsonProperty("has_more")
    private Boolean hasMore;

    @JsonProperty("next_cursor")
    private String nextCursor;
}
