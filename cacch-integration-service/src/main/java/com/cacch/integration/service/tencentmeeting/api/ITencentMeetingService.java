package com.cacch.integration.service.tencentmeeting.api;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;

/**
 * 腾讯会议服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ITencentMeetingService {

    /**
     * 查询单个云录制的智能纪要
     *
     * @param recordFileId    录制文件 ID
     * @param txMeetingUserId 腾讯会议 userid（已完成映射）
     * @return 智能纪要响应；纪要未就绪时返回 null
     */
    TencentMeetingSmartMinutesResponse getSmartMinutes(String recordFileId, String txMeetingUserId);
}
