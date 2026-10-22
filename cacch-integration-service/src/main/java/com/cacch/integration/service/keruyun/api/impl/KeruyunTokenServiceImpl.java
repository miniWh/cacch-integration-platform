package com.cacch.integration.service.keruyun.api.impl;

import com.cacch.integration.common.config.keruyun.KeruyunProperties;
import com.cacch.integration.common.constant.keruyun.KeruyunCommonConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.keruyun.client.KeruyunTokenClient;
import com.cacch.integration.integration.keruyun.client.dto.KeruyunTokenResponse;
import com.cacch.integration.service.keruyun.api.IKeruyunTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 客如云门店 token 服务实现：Redis 缓存读写 + 未命中回源
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeruyunTokenServiceImpl implements IKeruyunTokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final KeruyunProperties keruyunProperties;
    private final KeruyunTokenClient keruyunTokenClient;

    /**
     * 获取门店 token（Redis 缓存优先；客如云门店 token 当前固定不变，TTL 由配置控制）
     *
     * @return 门店 token
     * @throws BizException 鉴权配置缺失或客如云获取失败时抛出
     */
    @Override
    public String getAccessToken() {
        if (keruyunProperties.isAuthConfigured()) {
            log.info("【{}Token】获取终止, reason=鉴权配置不完整, appKeyConfigured={}, shopIdentyConfigured={}, appSecretConfigured={}",
                    KeruyunCommonConstants.LOG_BIZ,
                    !keruyunProperties.getAppKey().isBlank(),
                    !keruyunProperties.getShopIdenty().isBlank(),
                    !keruyunProperties.getAppSecret().isBlank());
            throw new BizException(ResultCode.PARAM_INVALID,
                    "客如云鉴权配置不完整：请检查 keruyun.app-key / keruyun.shop-identy / keruyun.app-secret");
        }

        String redisKey = KeruyunCommonConstants.tokenRedisKey(keruyunProperties.getAppKey(), keruyunProperties.getShopIdenty());

        String cachedToken = stringRedisTemplate.opsForValue().get(redisKey);
        if (StringUtils.hasText(cachedToken)) {
            log.info("【{}Token】Redis 缓存命中, key={}", KeruyunCommonConstants.LOG_BIZ, redisKey);
            return cachedToken;
        }

        log.info("【{}Token】Redis 缓存未命中, key={}, 将请求客如云 token 接口",
                KeruyunCommonConstants.LOG_BIZ, redisKey);

        KeruyunTokenResponse response = keruyunTokenClient.fetchToken();
        String token = response.getResult().getToken();

        stringRedisTemplate.opsForValue().set(redisKey, token, Duration.ofSeconds(keruyunProperties.getTokenCacheSeconds()));
        log.info("【{}Token】门店 token 已写入 Redis, key={}, ttl={}s",
                KeruyunCommonConstants.LOG_BIZ, redisKey, keruyunProperties.getTokenCacheSeconds());
        return token;
    }
}
