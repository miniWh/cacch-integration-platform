package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 企微 — 获取录制转写详情响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComGetTranscriptResponse extends WeComBaseResponse {

    @JsonProperty("has_more")
    private Boolean hasMore;

    private List<Map<String, Object>> transcripts;
}
