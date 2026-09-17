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
 * IHR Token 服务实现
 *
 * <p>支持两种 token 来源模式（yml {@code ihr.token-source}）：</p>
 * <ul>
 *     <li>{@code self}（默认）—— 自管理模式：集成平台自行申请/续期 token，
 *     缓存于 {@code integration:ihr:token} / {@code integration:ihr:refresh-token}</li>
 *     <li>{@code esb-redis} —— ESB 共享模式：只读 ESB（RestCloud iHR360 认证插件）写入
 *     Redis 的缓存 token（{@code ihr.esb-token-key} 配置的完整 key 及其派生 key）。
 *     正常流程集成平台<b>不申请 token</b>，保持 ESB 为唯一 token 生产者（Single Writer），
 *     避免双方互相作废对方缓存的 token；仅 {@link #forceRefresh()}（401 自愈）时
 *     申请新 token 并<b>覆盖写回 ESB 的 key</b>，写回结构/TTL 与 ESB 插件完全对齐</li>
 * </ul>
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

    /**
     * ESB 写回时的兜底 TTL（秒）—— 与 ESB 插件 normalizeExpireSeconds 对齐：
     * expires_in 缺失或非法时按 IHR 官方约定 2 小时处理
     */
    private static final int ESB_DEFAULT_EXPIRE_SECONDS = 7200;

    @Override
    public String getAccessToken() {
        if (ihrProperties.isEsbRedisSource()) {
            return getAccessTokenFromEsb();
        }
        return getAccessTokenSelf();
    }

    @Override
    public String forceRefresh() {
        if (ihrProperties.isEsbRedisSource()) {
            return forceRefreshEsb();
        }
        return forceRefreshSelf();
    }

    // ==================== ESB 共享模式（esb-redis） ====================

    /**
     * ESB 共享模式取 token —— 只读 ESB 缓存，绝不自行申请
     *
     * <p>缓存未命中时抛出业务异常（而非自行申请）：ESB 是唯一 token 生产者，
     * 集成平台自行申请会使 ESB 缓存中的 token 作废；此时应触发 ESB 完成一次
     * iHR 认证（或调用 {@link #forceRefresh()} 走覆盖写回自愈）。</p>
     */
    private String getAccessTokenFromEsb() {
        String accessKey = requireEsbTokenKey();
        String cached = stringRedisTemplate.opsForValue().get(accessKey);
        if (StringUtils.hasText(cached)) {
            log.info("【{}】ESB 缓存命中, key={}, 剩余秒数={}", BIZ, accessKey, esbRemainSeconds());
            return cached;
        }
        log.info("【{}】ESB 缓存未命中, key={}, 不自行申请（ESB 为唯一 token 生产者）", BIZ, accessKey);
        throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED,
                "IHR Token 缓存未命中（ESB 共享模式），请先触发 ESB 完成 iHR 认证: key=" + accessKey);
    }

    /**
     * ESB 共享模式强制刷新（401 自愈）—— 覆盖写回 ESB 缓存
     *
     * <p>优先用 ESB 缓存中的 refresh_token 续期，失败降级 client_credentials；
     * 成功后将 access_token / refresh_token / expires_at 覆盖写回 ESB 的 key，
     * key 结构与 TTL 语义均与 ESB 插件对齐，ESB 后续读取即可无缝拿到新 token。</p>
     */
    private String forceRefreshEsb() {
        IhrAppConfig credential = requireCredential();
        String accessKey = requireEsbTokenKey();
        String refreshKey = IhrConstants.esbRefreshTokenRedisKey(accessKey);

        // 优先尝试 ESB 缓存中的 refresh_token 续期
        String refreshToken = stringRedisTemplate.opsForValue().get(refreshKey);
        IhrTokenResponse response;
        if (StringUtils.hasText(refreshToken)) {
            log.info("【{}】ESB 模式尝试 refresh_token 续期", BIZ);
            response = safeRefresh(credential, refreshToken);
            if (response == null) {
                log.info("【{}】ESB 模式 refresh_token 续期失败，降级 client_credentials", BIZ);
                response = safeFetch(credential);
            }
        } else {
            log.info("【{}】ESB 缓存无 refresh_token，使用 client_credentials 获取", BIZ);
            response = safeFetch(credential);
        }

        if (response == null || !response.isSuccess()) {
            String detail = response == null ? "HTTP 调用失败"
                    : String.format("error=%s, desc=%s", response.getError(), response.getErrorDescription());
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED, "IHR Token 刷新失败（ESB 共享模式）: " + detail);
        }

        writeBackToEsb(accessKey, response);
        return response.getAccessToken();
    }

    /**
     * 将新 token 覆盖写回 ESB 缓存（与 ESB 插件 cacheTokens 结构/TTL 完全对齐）：
     * <ul>
     *     <li>{@code access key} = access_token，TTL = expires_in（缺失时兜底 7200）</li>
     *     <li>{@code refresh key} = refresh_token（响应携带时），TTL 同上</li>
     *     <li>{@code expires_at key} = 过期时间戳（epoch 秒），TTL 同上</li>
     * </ul>
     */
    private void writeBackToEsb(String accessKey, IhrTokenResponse response) {
        int ttlSeconds = normalizeExpireSeconds(response.getExpiresIn().intValue());
        long expireAt = System.currentTimeMillis() / 1000 + ttlSeconds;

        stringRedisTemplate.opsForValue().set(accessKey, response.getAccessToken(),
                Duration.ofSeconds(ttlSeconds));

        if (StringUtils.hasText(response.getRefreshToken())) {
            stringRedisTemplate.opsForValue().set(
                    IhrConstants.esbRefreshTokenRedisKey(accessKey), response.getRefreshToken(),
                    Duration.ofSeconds(ttlSeconds));
        }

        stringRedisTemplate.opsForValue().set(
                IhrConstants.esbExpiresAtRedisKey(accessKey), String.valueOf(expireAt),
                Duration.ofSeconds(ttlSeconds));

        log.info("【{}】token 已覆盖写回 ESB 缓存, key={}, ttl={}s, 过期时间戳={}", BIZ, accessKey, ttlSeconds, expireAt);
    }

    /**
     * 读取 ESB expires_at key 计算剩余秒数（仅用于日志观测，不参与决策）；
     * key 不存在或解析失败返回 -1
     */
    private long esbRemainSeconds() {
        try {
            String accessKey = requireEsbTokenKey();
            String expireAtValue = stringRedisTemplate.opsForValue()
                    .get(IhrConstants.esbExpiresAtRedisKey(accessKey));
            if (!StringUtils.hasText(expireAtValue)) {
                return -1;
            }
            return Long.parseLong(expireAtValue.trim()) - System.currentTimeMillis() / 1000;
        } catch (Exception e) {
            log.info("【{}】ESB expires_at 解析失败, reason={}", BIZ, e.getMessage());
            return -1;
        }
    }

    /**
     * 校验并返回 ESB access_token key —— esb-redis 模式下必须配置 {@code ihr.esb-token-key}
     */
    private String requireEsbTokenKey() {
        String key = ihrProperties.getEsbTokenKey();
        if (!StringUtils.hasText(key)) {
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED,
                    "ESB 共享模式未配置 ESB Token Key（ihr.esb-token-key）");
        }
        return key;
    }

    /**
     * TTL 归一化 —— 与 ESB 插件 normalizeExpireSeconds 对齐
     */
    private int normalizeExpireSeconds(Integer expiresIn) {
        if (expiresIn == null || expiresIn <= 0) {
            return ESB_DEFAULT_EXPIRE_SECONDS;
        }
        return expiresIn;
    }

    // ==================== 自管理模式（self，原有逻辑） ====================

    private String getAccessTokenSelf() {
        String key = IhrConstants.accessTokenRedisKey();
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            log.info("【{}】Redis 缓存命中, key={}", BIZ, key);
            return cached;
        }
        log.info("【{}】Redis 缓存未命中, key={}, 触发续期/重取", BIZ, key);
        return forceRefreshSelf();
    }

    private String forceRefreshSelf() {
        IhrAppConfig credential = requireCredential();

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

    // ==================== 公共工具 ====================

    private IhrAppConfig requireCredential() {
        IhrAppConfig credential = ihrProperties.getCredential();
        if (credential == null || !credential.isConfigured()) {
            log.info("【{}】刷新终止, reason=凭证未配置", BIZ);
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED, "IHR 凭证未配置（ihr.app-key / ihr.app-secret）");
        }
        return credential;
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
