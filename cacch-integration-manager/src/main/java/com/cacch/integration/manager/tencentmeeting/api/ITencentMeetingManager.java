package com.cacch.integration.manager.tencentmeeting.api;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;
import com.cacch.integration.manager.tencentmeeting.dto.TencentSessionRecordFile;

import java.util.List;

/**
 * 腾讯会议编排接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ITencentMeetingManager {

    /**
     * 通过企微会议号查询腾讯会议录制文件列表
     *
     * @param meetingCode     企微会议号
     * @param startTimeSec    查询起始时间（秒）
     * @param endTimeSec      查询结束时间（秒）
     * @param wecomOperatorId 企微 userid（内部会映射为腾讯会议 userid）
     * @return 腾讯会议录制文件列表
     */
    List<TencentSessionRecordFile> listSessionRecordFiles(String meetingCode, long startTimeSec, long endTimeSec,
                                                          String wecomOperatorId);

    /**
     * 查询单个云录制的智能纪要
     *
     * @param recordFileId    腾讯会议 record_file_id
     * @param wecomOperatorId 企微 userid（内部会映射为腾讯会议 userid）
     * @return 智能纪要响应；纪要未就绪时返回 null
     */
    TencentMeetingSmartMinutesResponse getSmartMinutes(String recordFileId, String wecomOperatorId);

    /**
     * 将企微 userid 映射为腾讯会议 userid
     *
     * @param wecomUserId 企微 userid
     * @return 腾讯会议 userid
     */
    String resolveTxMeetingUserId(String wecomUserId);
}
