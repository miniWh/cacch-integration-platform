package com.cacch.integration.service.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetRecordFileResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComListRecordResponse;
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
     * @param adminUserid           会议管理员 userid（业务上的会议创建人，取自参会人第一人）
     * @param title                 会议主题
     * @param meetingStartEpochSec  会议开始时间（Unix 秒）
     * @param durationMinutes       会议时长（分钟）
     * @param attendeeUserIds       参会人 userid 列表，可为空
     * @param description           会议描述，可为空
     * @param location              会议地点，可为空
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

    /**
     * 获取会议录制列表（支持分页）
     *
     * @param accessToken 企微 access_token
     * @param meetingId   企微会议 ID
     * @param startTimeSec 查询起始时间（Unix 秒）
     * @param endTimeSec   查询结束时间（Unix 秒）
     * @return 录制列表响应
     */
    WeComListRecordResponse listRecords(String accessToken, String meetingId, long startTimeSec, long endTimeSec);

    /**
     * 获取单个录制文件详情（含会议纪要下载地址）
     *
     * @param accessToken  企微 access_token
     * @param meetingId    企微会议 ID
     * @param recordFileId 录制文件 ID
     * @return 录制文件详情
     */
    WeComGetRecordFileResponse getRecordFile(String accessToken, String meetingId, String recordFileId);

    /**
     * 下载文本文件内容
     *
     * @param downloadUrl 下载地址
     * @return 文本内容
     */
    String downloadText(String downloadUrl);

    /**
     * 下载二进制文件
     *
     * @param downloadUrl 下载地址
     * @return 文件字节
     */
    byte[] downloadBytes(String downloadUrl);
}
