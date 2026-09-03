package com.cacch.integration.integration.ihr.client;

import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrConstants;
import com.cacch.integration.integration.ihr.client.dto.IhrTokenResponse;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * IHR 开放平台 OAuth2 Token HTTP 客户端
 *
 * <p>支持两种调用：
 * <ul>
 *     <li>{@link #fetchToken(String, String)} — 首次 / refresh_token 失效时使用（grant_type=client_credentials）</li>
 *     <li>{@link #refreshToken(String, String, String)} — refresh_token 续期（grant_type=refresh_token）</li>
 * </ul>
 *
 * <p>所有请求以 HTTP Basic（{@code base64(appKey:appSecret)}）方式鉴权，Header {@code Content-Type=application/x-www-form-urlencoded}。</p>
 *
 * <p>网关地址取自 {@link IhrProperties#getBaseUrl()}（yml {@code ihr.base-url}），
 * 与 {@link IhrConstants#TOKEN_PATH} 在运行时拼接；代码中不硬编码任何环境地址，
 * 测试 / 生产 / 内网域名切换只改配置，无需重新打包。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrTokenClient {

    private static final String BIZ = IhrConstants.LOG_BIZ;
    private static final String ACTION_FETCH = "获取 access_token";
    private static final String ACTION_REFRESH = "刷新 access_token";
    private static final String GRANT_TYPE_CLIENT = "client_credentials";
    private static final String GRANT_TYPE_REFRESH = "refresh_token";
    private static final String SCOPE = "client";
    private static final String PARAM_REFRESH_TOKEN = "refresh_token";

    private final RestTemplate restTemplate;
    private final IhrProperties ihrProperties;

    /**
     * 通过 client_credentials 模式获取 access_token / refresh_token
     *
     * @param appKey    IHR 分配的 appKey（用户名）
     * @param appSecret IHR 分配的 appSecret（密码）
     * @return Token 响应；含 access_token、refresh_token、expires_in
     * @throws RestClientException 网络或响应解析失败时抛出
     */
    public IhrTokenResponse fetchToken(String appKey, String appSecret) {
        return exchange(ACTION_FETCH, tokenUri(GRANT_TYPE_CLIENT, null), appKey, appSecret, null);
    }

    /**
     * 通过 refresh_token 模式续期 access_token
     *
     * @param appKey       IHR 分配的 appKey（用户名）
     * @param appSecret    IHR 分配的 appSecret（密码）
     * @param refreshToken 上次获取的 refresh_token
     * @return Token 响应（含新 access_token、新 refresh_token、expires_in）
     * @throws RestClientException 网络或响应解析失败时抛出
     */
    public IhrTokenResponse refreshToken(String appKey, String appSecret, String refreshToken) {
        return exchange(ACTION_REFRESH, tokenUri(GRANT_TYPE_REFRESH, refreshToken),
                appKey, appSecret, refreshToken);
    }

    /**
     * 拼接 Token 接口完整 URL：{@code IhrProperties#getBaseUrl() + IhrConstants#TOKEN_PATH}
     *
     * @param grantType    OAuth2 授权模式：client_credentials / refresh_token
     * @param refreshToken 续期用的 refresh_token；client_credentials 模式传 null
     * @return 带 query 参数的完整 URI
     */
    private URI tokenUri(String grantType, String refreshToken) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(ihrProperties.getBaseUrl() + IhrConstants.TOKEN_PATH)
                .queryParam("grant_type", grantType)
                .queryParam("scope", SCOPE);
        if (refreshToken != null) {
            builder.queryParam(PARAM_REFRESH_TOKEN, refreshToken);
        }
        return builder.build(true).toUri();
    }

    private IhrTokenResponse exchange(String action, URI uri, String appKey, String appSecret,
                                      String refreshToken) {
        if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
            throw new RestClientException("IHR 凭证未配置（ihr.app-key / ihr.app-secret）");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth(appKey, appSecret));

        ThirdPartyHttpLogSupport.logRequest(BIZ, action, uri.toString(),
                refreshToken == null ? Collections.emptyMap() : Collections.singletonMap("refresh_token", "****"));

        try {
            ResponseEntity<IhrTokenResponse> response = restTemplate.exchange(
                    uri, HttpMethod.POST, new HttpEntity<>(headers), IhrTokenResponse.class);
            IhrTokenResponse body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, action, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, action);
                throw new RestClientException("IHR Token 接口响应为空");
            }

            if (body.isSuccess()) {
                log.info("【{}】{}成功, expires_in={}s", BIZ, action, body.getExpiresIn());
            } else {
                log.info("【{}】{}终止, error={}, reason={}", BIZ, action, body.getError(),
                        body.getErrorDescription());
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, action, e);
            throw e;
        }
    }

    /**
     * 构造 IHR Basic 鉴权头：{@code base64(appKey:appSecret)}
     */
    private static String basicAuth(String appKey, String appSecret) {
        String raw = appKey + ":" + appSecret;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
