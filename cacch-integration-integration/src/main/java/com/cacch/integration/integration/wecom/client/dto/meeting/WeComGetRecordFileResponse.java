package com.cacch.integration.integration.wecom.client.dto.meeting;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 企微 — 获取单个录制文件详情响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComGetRecordFileResponse extends WeComBaseResponse {

    @JsonProperty("record_file_id")
    private String recordFileId;

    private String meetingid;

    @JsonProperty("meeting_code")
    private String meetingCode;

    @JsonProperty("meeting_summary")
    private Object meetingSummary;

    /**
     * 解析会议纪要文件列表（兼容 object / array 两种返回结构）
     *
     * @return 会议纪要文件列表，无数据时返回空列表
     */
    public List<WeComMeetingSummaryFileInfo> resolveMeetingSummaryFiles() {
        if (meetingSummary == null) {
            return List.of();
        }
        if (meetingSummary instanceof List<?> list) {
            List<WeComMeetingSummaryFileInfo> files = new ArrayList<>();
            for (Object item : list) {
                WeComMeetingSummaryFileInfo file = toSummaryFile(item);
                if (file != null) {
                    files.add(file);
                }
            }
            return files;
        }
        WeComMeetingSummaryFileInfo file = toSummaryFile(meetingSummary);
        return file != null ? List.of(file) : List.of();
    }

    private WeComMeetingSummaryFileInfo toSummaryFile(Object node) {
        if (!(node instanceof Map<?, ?> map)) {
            return null;
        }
        Object downloadAddress = map.get("download_address");
        Object fileType = map.get("file_type");
        if (downloadAddress == null && fileType == null) {
            return null;
        }
        WeComMeetingSummaryFileInfo file = new WeComMeetingSummaryFileInfo();
        if (downloadAddress != null) {
            file.setDownloadAddress(downloadAddress.toString());
        }
        if (fileType != null) {
            file.setFileType(fileType.toString());
        }
        return file;
    }
}
