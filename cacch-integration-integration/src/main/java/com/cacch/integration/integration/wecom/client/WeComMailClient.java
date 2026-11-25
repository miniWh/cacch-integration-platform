package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComMailLogConstants;
import com.cacch.integration.common.constant.wecom.WeComMailPathConstants;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListRequest;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListResponse;
import org.springframework.stereotype.Component;

/**
 * 企业微信邮件 HTTP 客户端
 *
 * <p>承载邮件域接口调用；access_token 内嵌在 URL query 参数中，
 * 由上层 Manager 解析应用配置后传入。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
public class WeComMailClient extends AbstractWeComClient {

    public WeComMailClient(RestTemplateHttpClient httpClient) {
        super(httpClient);
    }

    /**
     * 获取应用收件箱邮件列表
     *
     * @param accessToken 企微 access_token，不可为空
     * @param request     邮件列表查询条件（时间范围必填）
     * @return 邮件 ID 列表与翻页游标
     */
    public WeComMailListResponse getMailList(String accessToken, WeComMailListRequest request) {
        String url = String.format(WeComMailPathConstants.GET_MAIL_LIST_URL, accessToken);
        return postJson(url, request, WeComMailListResponse.class,
                WeComMailLogConstants.LOG_BIZ, WeComMailLogConstants.buildAction("获取收件箱邮件列表"));
    }
}
