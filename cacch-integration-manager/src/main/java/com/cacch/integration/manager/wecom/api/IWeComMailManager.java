package com.cacch.integration.manager.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListRequest;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListResponse;

/**
 * 企业微信邮件编排接口（对外唯一入口）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComMailManager {

    /**
     * 分页获取应用收件箱邮件列表
     *
     * @param request 查询条件，begin_time / end_time 必填
     * @return 邮件 ID 列表与翻页游标
     */
    WeComMailListResponse getMailList(WeComMailListRequest request);
}
