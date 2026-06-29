package com.cacch.integration.service.wecom.api;

import com.cacch.integration.common.exception.BizException;

/**
 * 企业微信 Token 服务接口
 *
 * @author cacch-integration
 */
public interface IWeComTokenService {

    /**
     * 获取企业微信 access_token（优先 Redis 缓存，miss 时调企微 API）
     *
     * @param corpid     企业 ID
     * @param appKey     业务标识（如 address-book、customer-contact）
     * @param corpsecret 应用的凭证密钥
     * @return access_token 值
     * @throws BizException 第三方系统调用失败时抛出
     */
    String getAccessToken(String corpid, String appKey, String corpsecret);
}
