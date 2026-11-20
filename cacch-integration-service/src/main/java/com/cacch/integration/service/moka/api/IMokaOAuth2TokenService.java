package com.cacch.integration.service.moka.api;

import com.cacch.integration.common.exception.BizException;

/**
 * Moka OAuth2 accessToken 服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaOAuth2TokenService {

    /**
     * 获取 accessToken（Redis 缓存优先）
     *
     * @return Bearer token
     * @throws BizException 未配置 OAuth 或远端失败
     */
    String getAccessToken();
}