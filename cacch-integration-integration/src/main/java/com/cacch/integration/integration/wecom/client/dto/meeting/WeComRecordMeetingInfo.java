package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 企微 — 会议录制列表项
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComRecordMeetingInfo {

    @JsonProperty("meeting_record_id")
    private String meetingRecordId;

    private String meetingid;

    @JsonProperty("meeting_code")
    private String meetingCode;

    @JsonProperty("host_user_id")
    private String hostUserId;

    @JsonProperty("meeting_start_time")
    private Long meetingStartTime;

    private String title;

    private Integer state;

    @JsonProperty("record_file_list")
    @JsonAlias("record_files")
    private List<WeComRecordFileInfo> recordFileList;
}
