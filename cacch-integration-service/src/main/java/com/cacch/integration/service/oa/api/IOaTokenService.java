package com.cacch.integration.service.oa.api;

/**
 * 致远 OA Token 服务（Redis 缓存）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaTokenService {

    /**
     * 获取 Rest Token（优先 Redis；未命中则调 OA 接口并缓存）
     *
     * @param loginName 绑定登录名，可空（空则用配置 {@code oa.default-login-name}）
     * @return Token 字符串
     */
    String getToken(String loginName);

    /**
     * 清除指定 loginName 的 Token 缓存（联调 / 401 后强制刷新）
     *
     * @param loginName 绑定登录名，可空
     */
    void evictToken(String loginName);
}
