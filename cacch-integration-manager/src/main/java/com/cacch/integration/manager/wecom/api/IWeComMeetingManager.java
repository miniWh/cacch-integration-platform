package com.cacch.integration.manager.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptResponse;

import java.util.List;

/**
 * 企业微信会议编排接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComMeetingManager {

    /**
     * 创建企微预约会议（内部自动获取 access_token）
     *
     * @param adminUserid           会议管理员 userid
     * @param title                 会议主题
     * @param meetingStartEpochSec  会议开始时间（Unix 秒）
     * @param durationMinutes       会议时长（分钟）
     * @param attendeeUserIds       参会人 userid 列表
     * @return 企微创建会议响应
     */
    WeComCreateMeetingResponse createMeeting(String adminUserid, String title,
                                             long meetingStartEpochSec, int durationMinutes,
                                             List<String> attendeeUserIds, String description);

    /**
     * 查询会议详情
     *
     * @param meetingId 企微会议 ID
     * @return 会议详情
     */
    WeComGetMeetingInfoResponse getMeetingInfo(String meetingId);

    /**
     * 获取会议录制转写详情
     *
     * @param meetingId    企微会议 ID
     * @param recordFileId 录制文件 ID
     * @return 转写详情
     */
    WeComGetTranscriptResponse getTranscriptDetail(String meetingId, String recordFileId);
}
