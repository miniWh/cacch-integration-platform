package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企微 — 会议纪要可下载文件信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComMeetingSummaryFileInfo {

    @JsonProperty("download_address")
    private String downloadAddress;

    @JsonProperty("file_type")
    private String fileType;
}
