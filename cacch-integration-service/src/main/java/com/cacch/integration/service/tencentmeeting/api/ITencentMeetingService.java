package com.cacch.integration.service.tencentmeeting.api;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingQueryResponse;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingRecordsResponse;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;

/**
 * 腾讯会议服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ITencentMeetingService {

    /**
     * 通过会议 Code 查询会议详情
     *
     * @param meetingCode     会议号
     * @param txMeetingUserId 腾讯会议 userid（已完成映射）
     * @return 会议查询响应
     * @throws com.cacch.integration.common.exception.BizException API 未启用或集成调用失败时抛出
     */
    TencentMeetingQueryResponse getMeetingByCode(String meetingCode, String txMeetingUserId);

    /**
     * 查询会议录制列表
     *
     * @param meetingId       腾讯会议 meeting_id
     * @param meetingCode     会议号
     * @param startTimeSec    查询起始时间（秒）
     * @param endTimeSec      查询结束时间（秒）
     * @param txMeetingUserId 腾讯会议 userid（已完成映射）
     * @return 录制列表响应
     * @throws com.cacch.integration.common.exception.BizException API 未启用或集成调用失败时抛出
     */
    TencentMeetingRecordsResponse listRecords(String meetingId, String meetingCode,
                                              long startTimeSec, long endTimeSec, String txMeetingUserId);

    /**
     * 查询单个云录制的智能纪要
     *
     * @param recordFileId    录制文件 ID
     * @param txMeetingUserId 腾讯会议 userid（已完成映射）
     * @return 智能纪要响应；纪要未就绪时返回 null
     * @throws com.cacch.integration.common.exception.BizException API 未启用或集成调用失败时抛出
     */
    TencentMeetingSmartMinutesResponse getSmartMinutes(String recordFileId, String txMeetingUserId);
}
