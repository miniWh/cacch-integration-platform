package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企微 — 会议录制文件信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComRecordFileInfo {

    @JsonProperty("record_file_id")
    private String recordFileId;

    @JsonProperty("record_start_time")
    private Long recordStartTime;

    @JsonProperty("record_end_time")
    private Long recordEndTime;

    @JsonProperty("record_size")
    private Integer recordSize;
}
