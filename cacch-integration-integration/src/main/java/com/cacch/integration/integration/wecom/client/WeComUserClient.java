package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import com.cacch.integration.integration.wecom.client.dto.user.WeComGetUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 企业微信通讯录 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComUserClient {

    private static final String BIZ = "WeComUser";
    private static final String ACTION = "读取成员详情";

    private final RestTemplateHttpClient httpClient;

    /**
     * 读取成员详情
     */
    public WeComGetUserResponse getUser(String accessToken, String userid) {
        String encodedUserId = UriUtils.encodeQueryParam(userid, StandardCharsets.UTF_8);
        String url = String.format(WeComConstants.USER_GET_URL, accessToken, encodedUserId);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));

        WeComGetUserResponse response = httpClient.doGet(url, headers, BIZ, ACTION, WeComGetUserResponse.class);

        // ThirdPartyHttpLogSupport 出入参日志已由 RestTemplateHttpClient 统一输出
        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION, url,
                ThirdPartyHttpLogSupport.queryParams("userid", userid));
        return response;
    }
}
