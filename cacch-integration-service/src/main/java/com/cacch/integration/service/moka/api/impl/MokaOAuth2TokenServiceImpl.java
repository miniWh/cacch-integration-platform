package com.cacch.integration.service.moka.api.impl;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.common.constant.moka.MokaPersonConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.moka.client.MokaOAuth2TokenClient;
import com.cacch.integration.integration.moka.client.dto.MokaOAuth2TokenResponse;
import com.cacch.integration.service.moka.api.IMokaOAuth2TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Moka OAuth2 accessToken 服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaOAuth2TokenServiceImpl implements IMokaOAuth2TokenService {

    private final MokaProperties mokaProperties;
    private final MokaOAuth2TokenClient tokenClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String getAccessToken() {
        if (!mokaProperties.isOAuthConfigured()) {
            log.info("【{}】OAuth2 获取终止, reason=clientId/clientSecret 未配置", MokaConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_INVALID, "Moka OAuth2 未配置，请设置 MOKA_OAUTH_CLIENT_ID / MOKA_OAUTH_CLIENT_SECRET");
        }

        String redisKey = MokaPersonConstants.oauthAccessTokenRedisKey();
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (StringUtils.hasText(cached)) {
            log.info("【{}】OAuth2 Redis 缓存命中", MokaConstants.LOG_BIZ);
            return cached;
        }

        MokaOAuth2TokenResponse response = tokenClient.getToken(
                mokaProperties.getOauthClientId(), mokaProperties.getOauthClientSecret());
        if (response == null || !response.isSuccess()
                || response.getData() == null || !StringUtils.hasText(response.getData().getAccessToken())) {
            log.info("【{}】OAuth2 获取失败, code={}, msg={}",
                    MokaConstants.LOG_BIZ,
                    response != null ? response.getCode() : null,
                    response != null ? response.getMsg() : null);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "Moka OAuth2 获取 accessToken 失败");
        }

        String token = response.getData().getAccessToken();
        long expiresIn = response.getData().getExpiresIn() != null ? response.getData().getExpiresIn() : 3600L;
        long ttl = Math.max(60L, expiresIn - MokaConstants.OAUTH2_TOKEN_TTL_BUFFER_SECONDS);
        stringRedisTemplate.opsForValue().set(redisKey, token, Duration.ofSeconds(ttl));
        log.info("【{}】OAuth2 accessToken 已写入 Redis, ttl={}s", MokaConstants.LOG_BIZ, ttl);
        return token;
    }
}