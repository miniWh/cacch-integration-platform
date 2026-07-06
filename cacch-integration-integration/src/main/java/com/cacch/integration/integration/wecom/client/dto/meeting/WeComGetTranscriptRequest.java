package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微 — 获取录制转写详情请求
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComGetTranscriptRequest {

    @JsonProperty("record_file_id")
    private String recordFileId;

    private String meetingid;

    private String pid;

    private Integer limit;
}
