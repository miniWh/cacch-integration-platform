package com.cacch.integration.manager.wecom.api;

import com.cacch.integration.common.exception.BizException;

/**
 * 企业微信 Token 编排接口（对外唯一入口）
 *
 * @author cacch-integration
 */
public interface IWeComTokenManager {

    /**
     * 获取企业微信 access_token（调用方无需接触 secret）
     *
     * @param corpid 企业 ID
     * @param appKey 业务标识（如 address-book、customer-contact）
     * @return access_token 值
     * @throws BizException 获取 token 失败时抛出
     */
    String getAccessToken(String corpid, String appKey);
}
