package com.cacch.integration.service.wecom.api.impl;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.WeComTokenClient;
import com.cacch.integration.integration.wecom.client.dto.WeComTokenResponse;
import com.cacch.integration.service.wecom.api.IWeComTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 企业微信 Token 服务实现：Redis 缓存读写 + miss 时调用 Client
 *
 * @author cacch-integration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComTokenServiceImpl implements IWeComTokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final WeComTokenClient weComTokenClient;

    @Override
    public String getAccessToken(String corpid, String appKey, String corpsecret) {
        // 动态生成 Redis Key：:wecom:token:{corpid}:{appKey}
        String redisKey = WeComConstants.tokenRedisKey(corpid, appKey);

        // 1. 先查 Redis 缓存
        String cachedToken = stringRedisTemplate.opsForValue().get(redisKey);

        if (cachedToken != null && !cachedToken.isBlank()) {
            log.info("【WeComToken】Redis 缓存命中, key={}", redisKey);
            return cachedToken;
        }

        // 2. 缓存未命中，调用企微 API
        log.info("【WeComToken】Redis 缓存未命中, key={}, 将请求企微 API", redisKey);

        WeComTokenResponse response = weComTokenClient.fetchToken(corpid, corpsecret);

        if (!response.isSuccess()) {
            throw new BizException(
                    ResultCode.INTEGRATION_AUTH_FAILED,
                    String.format("企业微信获取 access_token 失败, errcode=%d, errmsg=%s",
                            response.getErrCode(), response.getErrMsg())
            );
        }

        // 3. 写入 Redis 缓存（TTL = 7000s，略小于企微返回的 7200s）
        String token = response.getAccessToken();
        stringRedisTemplate.opsForValue().set(
                redisKey,
                token,
                Duration.ofSeconds(WeComConstants.TOKEN_TTL_SECONDS)
        );

        log.info("【WeComToken】access_token 已写入 Redis, key={}, ttl={}s",
                redisKey, WeComConstants.TOKEN_TTL_SECONDS);

        return token;
    }
}
