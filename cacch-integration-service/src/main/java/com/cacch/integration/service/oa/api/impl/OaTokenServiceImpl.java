package com.cacch.integration.service.oa.api.impl;

import com.cacch.integration.common.config.oa.OaProperties;
import com.cacch.integration.common.constant.oa.OaConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.oa.client.OaClient;
import com.cacch.integration.service.oa.api.IOaTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * 致远 OA Token 服务实现：Redis 缓存 + miss 时调 Client
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaTokenServiceImpl implements IOaTokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final OaClient oaClient;
    private final OaProperties oaProperties;

    @Override
    public String getToken(String loginName) {
        String resolvedLogin = resolveLoginName(loginName);
        String redisKey = OaConstants.tokenRedisKey(resolvedLogin);
        String cached = stringRedisTemplate.opsForValue().get(redisKey);
        if (StringUtils.hasText(cached)) {
            log.info("【OaOpenApi】Token 缓存命中, key={}", redisKey);
            return cached;
        }
        log.info("【OaOpenApi】Token 缓存未命中, key={}, 将请求致远 OA", redisKey);
        try {
            String token = oaClient.fetchToken(resolvedLogin);
            long ttl = Math.max(60L, oaProperties.getTokenTtlSeconds());
            stringRedisTemplate.opsForValue().set(redisKey, token, Duration.ofSeconds(ttl));
            log.info("【OaOpenApi】Token 已写入 Redis, key={}, ttl={}s", redisKey, ttl);
            return token;
        } catch (RestClientException e) {
            log.info("【OaOpenApi】获取 Token 终止, loginName={}, reason={}", resolvedLogin, e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED,
                    "致远 OA 获取 Token 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void evictToken(String loginName) {
        String redisKey = OaConstants.tokenRedisKey(resolveLoginName(loginName));
        Boolean deleted = stringRedisTemplate.delete(redisKey);
        log.info("【OaOpenApi】清除 Token 缓存, key={}, deleted={}", redisKey, deleted);
    }

    private String resolveLoginName(String loginName) {
        if (StringUtils.hasText(loginName)) {
            return loginName.trim();
        }
        if (StringUtils.hasText(oaProperties.getDefaultLoginName())) {
            return oaProperties.getDefaultLoginName().trim();
        }
        return null;
    }
}
