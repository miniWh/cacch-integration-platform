package com.cacch.integration.integration.tencentmeeting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 腾讯会议录制列表响应（/v1/records）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TencentMeetingRecordsResponse {

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("current_size")
    private Integer currentSize;

    @JsonProperty("current_page")
    private Integer currentPage;

    @JsonProperty("total_page")
    private Integer totalPage;

    @JsonProperty("record_meetings")
    private List<RecordMeeting> recordMeetings;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecordMeeting {

        @JsonProperty("meeting_record_id")
        private String meetingRecordId;

        @JsonProperty("meeting_id")
        private String meetingId;

        @JsonProperty("meeting_code")
        private String meetingCode;

        private Integer state;

        @JsonProperty("record_files")
        private List<RecordFile> recordFiles;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecordFile {

        @JsonProperty("record_file_id")
        private String recordFileId;

        @JsonProperty("record_start_time")
        private Long recordStartTime;

        @JsonProperty("record_end_time")
        private Long recordEndTime;
    }
}
