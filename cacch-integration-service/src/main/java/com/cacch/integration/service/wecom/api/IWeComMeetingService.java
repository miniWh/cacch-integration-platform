package com.cacch.integration.service.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptResponse;

import java.util.List;

/**
 * 企业微信会议服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComMeetingService {

    /**
     * 创建企微预约会议
     *
     * @param accessToken           企微 access_token
     * @param adminUserid           会议管理员 userid
     * @param title                 会议主题
     * @param meetingStartEpochSec  会议开始时间（Unix 秒）
     * @param durationMinutes       会议时长（分钟）
     * @param attendeeUserIds       参会人 userid 列表，可为空
     * @return 企微创建会议响应
     */
    WeComCreateMeetingResponse createMeeting(String accessToken, String adminUserid, String title,
                                             long meetingStartEpochSec, int durationMinutes,
                                             List<String> attendeeUserIds, String description,
                                             String location);

    /**
     * 查询会议详情
     *
     * @param accessToken 企微 access_token
     * @param meetingId   企微会议 ID
     * @return 会议详情
     */
    WeComGetMeetingInfoResponse getMeetingInfo(String accessToken, String meetingId);

    /**
     * 获取会议录制转写详情
     *
     * @param accessToken  企微 access_token
     * @param meetingId    企微会议 ID
     * @param recordFileId 录制文件 ID
     * @return 转写详情
     */
    WeComGetTranscriptResponse getTranscriptDetail(String accessToken, String meetingId, String recordFileId);
}
