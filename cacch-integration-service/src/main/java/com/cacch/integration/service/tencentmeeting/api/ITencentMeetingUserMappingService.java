package com.cacch.integration.service.tencentmeeting.api;

/**
 * 企微 userid 与腾讯会议 userid 映射服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ITencentMeetingUserMappingService {

    /**
     * 将企微 userid 转换为腾讯会议 userid
     *
     * @param wecomUserId 企微 userid
     * @return 腾讯会议 userid；未找到映射时返回 null
     */
    String resolveTxMeetingUserId(String wecomUserId);
}
