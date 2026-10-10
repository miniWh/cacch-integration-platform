package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import com.cacch.integration.integration.wecom.client.dto.WeComTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * 企业微信 access_token HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担；
 * 企微鉴权特殊：access_token 直接内嵌在 URL query 参数中，无需 Header。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComTokenClient {

    private static final String BIZ = "WeComToken";
    private static final String ACTION = "获取 access_token";

    private final RestTemplateHttpClient httpClient;

    /**
     * 调用企微 /gettoken 接口获取 access_token
     *
     * @param corpid     企业 ID
     * @param corpsecret 应用的凭证密钥
     * @return 企微 token 响应（含 access_token）
     * @throws RestClientException 网络或 HTTP 错误
     */
    public WeComTokenResponse fetchToken(String corpid, String corpsecret) {
        String url = String.format(WeComConstants.TOKEN_URL, corpid, corpsecret);
        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION, url,
                ThirdPartyHttpLogSupport.queryParams("corpid", corpid, "corpsecret", "****"));

        try {
            WeComTokenResponse response = httpClient.doGet(url, null, BIZ, ACTION, WeComTokenResponse.class);

            if (response.isSuccess()) {
                log.info("【WeComToken】access_token 获取成功, expires_in={}s", response.getExpiresIn());
            } else {
                log.info("【WeComToken】获取 token 终止, corpid={}, errcode={}, reason={}",
                        maskString(corpid, 6), response.getErrCode(), response.getErrMsg());
            }
            return response;

        } catch (RestClientException e) {
            log.info("【WeComToken】获取 token 终止, corpid={}, reason={}",
                    maskString(corpid, 6), e.getMessage());
            log.error("【WeComToken】HTTP 调用失败, url={}", ThirdPartyHttpLogSupport.maskUrl(url), e);
            throw e;
        }
    }

    private String maskString(String value, int keepLength) {
        if (value == null || value.length() <= keepLength) {
            return "****";
        }
        return value.substring(0, keepLength) + "****";
    }
}
