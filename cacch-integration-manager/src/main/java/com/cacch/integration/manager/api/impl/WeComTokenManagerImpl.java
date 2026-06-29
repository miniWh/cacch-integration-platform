package com.cacch.integration.manager.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.common.config.WeComAppConfig;
import com.cacch.integration.common.config.WeComProperties;
import com.cacch.integration.manager.api.IWeComTokenManager;
import com.cacch.integration.service.api.IWeComTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * 企业微信 Token 编排实现 — 内部通过 WeComProperties 解析 secret
 *
 * @author cacch-integration
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComTokenManagerImpl implements IWeComTokenManager {

    private final IWeComTokenService weComTokenService;
    private final WeComProperties weComProperties;

    @Override
    public String getAccessToken(String corpid, String appKey) {
        // 1. 从配置中解析 secret（调用方无需接触 secret）
        WeComAppConfig appConfig = weComProperties.findByCorpidAndAppKey(corpid, appKey)
                .orElseThrow(() -> {
                    log.error("[WeComToken] 未找到匹配的企业微信应用配置, corpid={}, appKey={}", corpid, appKey);
                    return new BizException(ResultCode.PARAM_INVALID,
                            String.format("未找到企业微信应用配置: corpid=%s, appKey=%s", corpid, appKey));
                });

        try {
            String token = weComTokenService.getAccessToken(appConfig.getCorpid(), appConfig.getAppKey(),
                    appConfig.getSecret());
            log.info("[WeComToken] 编排层获取 token 成功, corpid={}, appKey={}", corpid, appKey);
            return token;
        } catch (BizException e) {
            log.error("[WeComToken] 编排层获取 token 失败, corpid={}, appKey={}, errCode={}, errMsg={}",
                    corpid, appKey, e.getCode(), e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.error("[WeComToken] HTTP 调用异常, corpid={}, appKey={}", corpid, appKey, e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "企业微信接口超时", e);
        } catch (Exception e) {
            log.error("[WeComToken] 获取 token 发生未知异常, corpid={}, appKey={}", corpid, appKey, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "获取企业微信 access_token 失败", e);
        }
    }
}
