package com.cacch.integration.service.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListRequest;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListResponse;

/**
 * 企业微信邮件服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComMailService {

    /**
     * 分页获取应用收件箱邮件列表
     *
     * @param accessToken 企微 access_token，不可为空
     * @param request     查询条件，begin_time / end_time 必填
     * @return 邮件 ID 列表与翻页游标
     */
    WeComMailListResponse getMailList(String accessToken, WeComMailListRequest request);
}
