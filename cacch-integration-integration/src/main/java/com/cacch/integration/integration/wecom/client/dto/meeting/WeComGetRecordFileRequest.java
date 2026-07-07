package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微 — 获取单个录制文件详情请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComGetRecordFileRequest {

    @JsonProperty("record_file_id")
    private String recordFileId;

    private String meetingid;
}
