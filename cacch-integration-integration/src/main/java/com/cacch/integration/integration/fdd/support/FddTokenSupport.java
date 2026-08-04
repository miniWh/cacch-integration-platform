package com.cacch.integration.integration.fdd.support;

import com.cacch.integration.common.config.fdd.FddProperties;
import com.cacch.integration.common.constant.fdd.FddConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.fdd.client.dto.FddTokenResponse;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 法大大 OAuth2 Token 获取与进程内缓存（加密模式）
 *
 * <p>sign = SHA256(timestamp + appSecret).toUpperCase()</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FddTokenSupport {

    private static final String ACTION = "获取 accessToken";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final RestTemplate restTemplate;
    private final FddProperties fddProperties;

    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    /**
     * 获取 accessToken（优先本地缓存，过期后重新拉取）
     *
     * @return accessToken
     * @throws BizException 配置缺失或法大大鉴权失败时抛出
     */
    public String getAccessToken() {
        CachedToken current = cachedToken.get();
        long now = System.currentTimeMillis();
        if (current != null && current.expireAtMillis() > now) {
            log.info("【{}】Token 本地缓存命中, appId={}", FddConstants.LOG_BIZ, fddProperties.getAppId());
            return current.token();
        }

        if (!StringUtils.hasText(fddProperties.getAppId())
                || !StringUtils.hasText(fddProperties.getAppSecret())
                || !StringUtils.hasText(fddProperties.getAuthUrl())) {
            log.info("【{}】获取 Token 终止, reason=appId/appSecret/authUrl 未配置", FddConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_INVALID, "法大大鉴权配置不完整");
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = sha256Upper(timestamp + fddProperties.getAppSecret());

        Map<String, String> body = new LinkedHashMap<>();
        body.put("appId", fddProperties.getAppId());
        body.put("sign", sign);
        body.put("timestamp", timestamp);

        String url = fddProperties.getAuthUrl();
        try {
            String bodyJson = OBJECT_MAPPER.writeValueAsString(body);
            ThirdPartyHttpLogSupport.logRequest(FddConstants.LOG_BIZ, ACTION, url, Map.of(
                    "appId", fddProperties.getAppId(),
                    "sign", "****",
                    "timestamp", timestamp
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            FddTokenResponse response = restTemplate.postForObject(url, entity, FddTokenResponse.class);
            ThirdPartyHttpLogSupport.logResponse(FddConstants.LOG_BIZ, ACTION, response);

            if (response == null || !response.isSuccess()) {
                String msg = response == null ? "响应为空"
                        : "code=" + response.getCode() + ", message=" + response.getMessage();
                log.info("【{}】获取 Token 终止, reason={}", FddConstants.LOG_BIZ, msg);
                throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED, "法大大获取 accessToken 失败: " + msg);
            }

            String token = response.getData().getAccessToken();
            long cacheMillis = Math.max(1, fddProperties.getTokenCacheMinutes()) * 60_000L;
            cachedToken.set(new CachedToken(token, now + cacheMillis));
            log.info("【{}】accessToken 获取成功并缓存, appId={}, cacheMinutes={}",
                    FddConstants.LOG_BIZ, fddProperties.getAppId(), fddProperties.getTokenCacheMinutes());
            return token;
        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】获取 Token 终止, reason={}", FddConstants.LOG_BIZ, e.getMessage());
            log.error("【{}】获取 Token HTTP 失败, url={}", FddConstants.LOG_BIZ, ThirdPartyHttpLogSupport.maskUrl(url), e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "法大大获取 accessToken 超时或网络异常", e);
        } catch (Exception e) {
            log.info("【{}】获取 Token 终止, reason={}", FddConstants.LOG_BIZ, e.getMessage());
            log.error("【{}】获取 Token 处理失败", FddConstants.LOG_BIZ, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大获取 accessToken 失败", e);
        }
    }

    private static String sha256Upper(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private record CachedToken(String token, long expireAtMillis) {
    }
}
