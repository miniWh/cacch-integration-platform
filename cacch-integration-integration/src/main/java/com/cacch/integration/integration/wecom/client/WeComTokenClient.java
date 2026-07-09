package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import com.cacch.integration.integration.wecom.client.dto.WeComTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 企业微信 access_token HTTP 客户端
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComTokenClient {

    private static final String BIZ = "WeComToken";
    private static final String ACTION = "获取 access_token";

    private final RestTemplate restTemplate;

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
            WeComTokenResponse response = restTemplate.getForObject(url, WeComTokenResponse.class);
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION, response);

            if (response == null) {
                log.info("【WeComToken】获取 token 终止, corpid={}, reason=接口返回null", maskString(corpid, 6));
                log.error("【WeComToken】接口返回 null");
                throw new RestClientException("企业微信 /gettoken 返回 null");
            }

            if (response.isSuccess()) {
                log.info("【WeComToken】access_token 获取成功, expires_in={}s", response.getExpiresIn());
            } else {
                log.info("【WeComToken】获取 token 终止, corpid={}, errcode={}, reason={}",
                        maskString(corpid, 6), response.getErrCode(), response.getErrMsg());
                log.error("【WeComToken】企微返回错误, errcode={}, errmsg={}",
                        response.getErrCode(), response.getErrMsg());
            }

            return response;

        } catch (RestClientException e) {
            log.info("【WeComToken】获取 token 终止, corpid={}, reason={}",
                    maskString(corpid, 6), e.getMessage());
            log.error("【WeComToken】HTTP 调用失败, url={}", ThirdPartyHttpLogSupport.maskUrl(url), e);
            throw e;
        }
    }

    /**
     * 敏感信息脱敏：保留前 n 位，其余用 * 代替
     */
    private String maskString(String value, int keepLength) {
        if (value == null || value.length() <= keepLength) {
            return "****";
        }
        return value.substring(0, keepLength) + "****";
    }
}
