package com.cacch.integration.service.ihr.api.impl;

import com.cacch.integration.common.config.ihr.IhrAppConfig;
import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.IhrTokenClient;
import com.cacch.integration.integration.ihr.client.dto.IhrTokenResponse;
import com.cacch.integration.service.ihr.api.IIhrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * IHR Token 服务实现：Redis 缓存 + refresh_token 续期 + 失效重取
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IhrTokenServiceImpl implements IIhrTokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final IhrTokenClient ihrTokenClient;
    private final IhrProperties ihrProperties;

    private static final String BIZ = "IHR Token 服务实现";

    @Override
    public String getAccessToken() {
        String key = IhrConstants.accessTokenRedisKey();
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            log.info("【{}】Redis 缓存命中, key={}", BIZ, key);
            return cached;
        }
        log.info("【{}】Redis 缓存未命中, key={}, 触发续期/重取", BIZ, key);
        return forceRefresh();
    }

    @Override
    public String forceRefresh() {
        IhrAppConfig credential = ihrProperties.getCredential();
        if (credential == null || !credential.isConfigured()) {
            log.info("【{}】强制刷新终止, reason=凭证未配置", BIZ);
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED, "IHR 凭证未配置（ihr.app-key / ihr.app-secret）");
        }

        String refreshKey = IhrConstants.refreshTokenRedisKey();
        String refreshToken = stringRedisTemplate.opsForValue().get(refreshKey);

        IhrTokenResponse response;
        if (StringUtils.hasText(refreshToken)) {
            log.info("【{}】尝试 refresh_token 续期", BIZ);
            response = safeRefresh(credential, refreshToken);
            if (response == null) {
                log.info("【{}】refresh_token 续期失败，降级为 client_credentials", BIZ);
                response = safeFetch(credential);
            }
        } else {
            log.info("【{}】无 refresh_token 缓存，使用 client_credentials 获取", BIZ);
            response = safeFetch(credential);
        }

        if (response == null) {
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED, "IHR Token 获取失败");
        }
        if (!response.isSuccess()) {
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED,
                    String.format("IHR Token 获取失败: error=%s, desc=%s",
                            response.getError(), response.getErrorDescription()));
        }

        cacheTokens(response);
        return response.getAccessToken();
    }

    private IhrTokenResponse safeFetch(IhrAppConfig credential) {
        try {
            return ihrTokenClient.fetchToken(credential.getAppKey(), credential.getAppSecret());
        } catch (Exception e) {
            log.info("【{}】client_credentials 获取终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】client_credentials HTTP 调用失败", BIZ, e);
            return null;
        }
    }

    private IhrTokenResponse safeRefresh(IhrAppConfig credential, String refreshToken) {
        try {
            return ihrTokenClient.refreshToken(credential.getAppKey(), credential.getAppSecret(), refreshToken);
        } catch (Exception e) {
            log.info("【{}】refresh_token 续期终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】refresh_token HTTP 调用失败", BIZ, e);
            return null;
        }
    }

    private void cacheTokens(IhrTokenResponse response) {
        String accessKey = IhrConstants.accessTokenRedisKey();
        String refreshKey = IhrConstants.refreshTokenRedisKey();

        stringRedisTemplate.opsForValue().set(accessKey, response.getAccessToken(),
                Duration.ofSeconds(IhrConstants.ACCESS_TOKEN_CACHE_TTL_SECONDS));

        if (StringUtils.hasText(response.getRefreshToken())) {
            stringRedisTemplate.opsForValue().set(refreshKey, response.getRefreshToken(),
                    Duration.ofSeconds(IhrConstants.REFRESH_TOKEN_CACHE_TTL_SECONDS));
        }

        log.info("【{}】token 已写入 Redis, access_ttl={}s, refresh_ttl={}s, expires_in={}s", BIZ,
                IhrConstants.ACCESS_TOKEN_CACHE_TTL_SECONDS,
                IhrConstants.REFRESH_TOKEN_CACHE_TTL_SECONDS,
                response.getExpiresIn());
    }
}
