package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import com.cacch.integration.integration.wecom.client.dto.user.WeComGetUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * 企业微信通讯录 HTTP 客户端
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComUserClient {

    private static final String BIZ = "WeComUser";
    private static final String ACTION = "读取成员详情";

    private final RestTemplate restTemplate;

    /**
     * 读取成员详情
     *
     * @param accessToken 企微 access_token
     * @param userid      成员 UserID，不可为空
     * @return 成员详情响应（含 name）
     * @throws RestClientException 网络或 HTTP 错误
     */
    public WeComGetUserResponse getUser(String accessToken, String userid) {
        String encodedUserId = UriUtils.encodeQueryParam(userid, StandardCharsets.UTF_8);
        String url = String.format(WeComConstants.USER_GET_URL, accessToken, encodedUserId);
        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION, url,
                ThirdPartyHttpLogSupport.queryParams("userid", userid));
        try {
            WeComGetUserResponse response = restTemplate.getForObject(url, WeComGetUserResponse.class);
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION, response);
            if (response == null) {
                log.info("【WeComUser】读取成员详情终止, userid={}, reason=接口返回null", userid);
                throw new RestClientException("企业微信读取成员详情返回 null");
            }
            return response;
        } catch (RestClientException e) {
            log.info("【WeComUser】读取成员详情终止, userid={}, reason={}", userid, e.getMessage());
            log.error("【WeComUser】读取成员详情 HTTP 调用失败, userid={}", userid, e);
            throw e;
        }
    }
}
