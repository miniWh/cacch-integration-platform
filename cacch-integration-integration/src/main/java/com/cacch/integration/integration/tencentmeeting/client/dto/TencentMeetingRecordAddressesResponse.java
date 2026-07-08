package com.cacch.integration.integration.tencentmeeting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 腾讯会议录制地址查询响应（/v1/addresses）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TencentMeetingRecordAddressesResponse {

    @JsonProperty("meeting_record_id")
    private String meetingRecordId;

    @JsonProperty("record_files")
    private List<RecordFile> recordFiles;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecordFile {

        @JsonProperty("record_file_id")
        private String recordFileId;
    }
}
