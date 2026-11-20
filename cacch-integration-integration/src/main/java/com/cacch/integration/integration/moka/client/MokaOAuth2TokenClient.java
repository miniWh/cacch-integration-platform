package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaOAuth2TokenRequest;
import com.cacch.integration.integration.moka.client.dto.MokaOAuth2TokenResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Moka OAuth2 Token HTTP 客户端
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaOAuth2TokenClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION = "Moka OAuth2 获取Token";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 获取 accessToken
     *
     * @param clientId     OAuth clientID
     * @param clientSecret OAuth clientSecret
     * @return Token 响应
     */
    public MokaOAuth2TokenResponse getToken(String clientId, String clientSecret) {
        String url = mokaProperties.getBaseUrl() + MokaConstants.OAUTH2_TOKEN_PATH;
        MokaOAuth2TokenRequest request = new MokaOAuth2TokenRequest();
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        request.setGrantType(MokaConstants.OAUTH2_GRANT_TYPE);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        MokaOAuth2TokenResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ, ACTION, MokaOAuth2TokenResponse.class);

        if (body != null && body.isSuccess()) {
            log.info("【{}】{}成功", BIZ, ACTION);
        } else if (body != null) {
            log.info("【{}】{}终止, code={}, msg={}", BIZ, ACTION, body.getCode(), body.getMsg());
        }
        return body;
    }
}