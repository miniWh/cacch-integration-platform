package com.cacch.integration.service.ihr.api;

import com.cacch.integration.common.exception.BizException;

/**
 * IHR 开放平台 Token 服务接口
 *
 * <p>负责 access_token / refresh_token 的获取、缓存、续期，对外仅暴露 access_token 字符串。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrTokenService {

    /**
     * 获取可用的 IHR access_token。
     *
     * <p>策略：
     * <ol>
     *     <li>优先读 Redis 缓存（key=integration:ihr:token）；命中即返回；</li>
     *     <li>缓存未命中：使用 refresh_token 续期；refresh_token 不存在或失效时降级为 client_credentials 全量获取；</li>
     *     <li>刷新结果写回 Redis，TTL 略短于 IHR 官方有效期（防临界过期）。</li>
     * </ol>
     *
     * @return access_token 字符串
     * @throws BizException IHR 凭证未配置或调用失败时抛出
     */
    String getAccessToken();

    /**
     * 强制刷新 access_token（先尝试 refresh_token 续期，失败时回退到 client_credentials）。
     *
     * <p>由调用方在收到 401/403/业务 code 非 0 时手动触发，刷新成功后下一次 {@link #getAccessToken()}
     * 即返回新 token。</p>
     *
     * @return 新的 access_token 字符串
     * @throws BizException IHR 凭证未配置或调用失败时抛出
     */
    String forceRefresh();
}
