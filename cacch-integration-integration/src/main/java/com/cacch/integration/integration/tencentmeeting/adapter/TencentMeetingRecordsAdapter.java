package com.cacch.integration.integration.tencentmeeting.adapter;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingQueryResponse;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingRecordsResponse;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 腾讯会议录制列表解析适配器
 *
 * @author hongfu_zhou@cacch.com
 */
public final class TencentMeetingRecordsAdapter {

    /** 转码完成 */
    public static final int RECORD_STATE_TRANSCODED = 3;

    private TencentMeetingRecordsAdapter() {
    }

    /**
     * 从会议查询响应中解析 meeting_id
     *
     * @param response    会议查询响应
     * @param meetingCode 会议号（用于精确匹配）
     * @return meeting_id；无法解析时返回 null
     */
    public static String resolveMeetingId(TencentMeetingQueryResponse response, String meetingCode) {
        if (response == null || response.getMeetingInfoList() == null || response.getMeetingInfoList().isEmpty()) {
            return null;
        }
        List<TencentMeetingQueryResponse.MeetingInfo> meetingInfoList = response.getMeetingInfoList();
        if (StringUtils.hasText(meetingCode)) {
            for (TencentMeetingQueryResponse.MeetingInfo meetingInfo : meetingInfoList) {
                if (meetingCode.equals(normalizeMeetingCode(meetingInfo.getMeetingCode()))
                        && StringUtils.hasText(meetingInfo.getMeetingId())) {
                    return meetingInfo.getMeetingId();
                }
            }
        }
        TencentMeetingQueryResponse.MeetingInfo first = meetingInfoList.getFirst();
        return StringUtils.hasText(first.getMeetingId()) ? first.getMeetingId() : null;
    }

    /**
     * 将录制列表响应展开为 record_file 列表
     *
     * @param response 录制列表响应
     * @return 录制文件列表
     */
    public static List<TencentMeetingRecordsResponse.RecordFile> flattenRecordFiles(
            TencentMeetingRecordsResponse response) {
        if (response == null || response.getRecordMeetings() == null || response.getRecordMeetings().isEmpty()) {
            return List.of();
        }
        List<TencentMeetingRecordsResponse.RecordFile> files = new ArrayList<>();
        for (TencentMeetingRecordsResponse.RecordMeeting recordMeeting : response.getRecordMeetings()) {
            if (recordMeeting.getRecordFiles() == null || recordMeeting.getRecordFiles().isEmpty()) {
                continue;
            }
            for (TencentMeetingRecordsResponse.RecordFile recordFile : recordMeeting.getRecordFiles()) {
                if (StringUtils.hasText(recordFile.getRecordFileId())) {
                    files.add(recordFile);
                }
            }
        }
        return Collections.unmodifiableList(files);
    }

    /**
     * 判断录制项是否仍在转码
     *
     * @param state 腾讯会议录制状态
     * @return true 表示转码中或未就绪
     */
    public static boolean isTranscoding(Integer state) {
        return state != null && state != RECORD_STATE_TRANSCODED;
    }

    /**
     * 规范化会议号：去除空格和连字符
     *
     * @param meetingCode 原始会议号
     * @return 规范化后的会议号
     */
    public static String normalizeMeetingCode(String meetingCode) {
        if (!StringUtils.hasText(meetingCode)) {
            return meetingCode;
        }
        return meetingCode.trim().replace(" ", "").replace("-", "");
    }
}
