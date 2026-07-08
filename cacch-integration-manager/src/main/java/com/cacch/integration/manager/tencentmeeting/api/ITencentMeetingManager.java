package com.cacch.integration.manager.tencentmeeting.api;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;

/**
 * 腾讯会议编排接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ITencentMeetingManager {

    /**
     * 查询单个云录制的智能纪要
     *
     * @param recordFileId     录制文件 ID
     * @param wecomOperatorId  企微 userid（内部会映射为腾讯会议 userid）
     * @return 智能纪要响应；纪要未就绪时返回 null
     */
    TencentMeetingSmartMinutesResponse getSmartMinutes(String recordFileId, String wecomOperatorId);

    /**
     * 将企微录制标识解析为腾讯会议 record_file_id
     *
     * @param meetingRecordId   企微 meeting_record_id
     * @param wecomRecordFileId 企微 record_file_id
     * @param sessionIndex      场次序号（从 1 开始）
     * @param wecomOperatorId   企微 userid
     * @return 腾讯会议 record_file_id；无法解析时返回 null
     */
    String resolveTencentRecordFileId(String meetingRecordId, String wecomRecordFileId, int sessionIndex,
                                      String wecomOperatorId);

    /**
     * 将企微 userid 映射为腾讯会议 userid
     *
     * @param wecomUserId 企微 userid
     * @return 腾讯会议 userid
     */
    String resolveTxMeetingUserId(String wecomUserId);
}
