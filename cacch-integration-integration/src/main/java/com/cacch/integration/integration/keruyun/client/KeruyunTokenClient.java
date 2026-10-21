package com.cacch.integration.integration.keruyun.client;

import com.cacch.integration.common.config.keruyun.KeruyunProperties;
import com.cacch.integration.common.constant.keruyun.KeruyunConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.keruyun.client.dto.KeruyunTokenResponse;
import com.cacch.integration.integration.keruyun.support.KeruyunSignSupport;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 客如云获取门店 token 客户端
 *
 * <p>接口：{@code GET /open/v1/token/get}，签名为 SHA-256(公共参数串 + secretKey)；
 * HTTP 调用与出入参日志由 {@link RestTemplateHttpClient} 统一承担。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeruyunTokenClient {

    private static final String ACTION = "获取门店token";

    private final RestTemplateHttpClient httpClient;
    private final KeruyunProperties keruyunProperties;
    private final KeruyunSignSupport keruyunSignSupport;

    /**
     * 获取门店 token（不做缓存，缓存由 Service 层 Redis 承担）
     *
     * @return 客如云 token 响应，已校验 code=0 且 result.token 非空
     * @throws BizException 鉴权配置缺失、客如云返回非成功码或网络异常时抛出
     */
    public KeruyunTokenResponse fetchToken() {
        if (keruyunProperties.isAuthConfigured()) {
            log.info("【{}】{}终止, reason=鉴权配置不完整, appKeyConfigured={}, shopIdentyConfigured={}, appSecretConfigured={}",
                    KeruyunConstants.LOG_BIZ,
                    ACTION,
                    !keruyunProperties.getAppKey().isBlank(),
                    !keruyunProperties.getShopIdenty().isBlank(),
                    !keruyunProperties.getAppSecret().isBlank());
            throw new BizException(ResultCode.PARAM_INVALID,
                    "客如云鉴权配置不完整：请检查 keruyun.app-key / keruyun.shop-identy / keruyun.app-secret");
        }

        String timestamp = keruyunSignSupport.currentTimestampSeconds();
        Map<String, String> publicParams = keruyunSignSupport.buildPublicParams(timestamp);
        String sign = keruyunSignSupport.signForToken(publicParams);
        String url = buildUrl(publicParams, sign);

        try {
            KeruyunTokenResponse response = httpClient.doGet(url, new HttpHeaders(),
                    KeruyunConstants.LOG_BIZ, ACTION, KeruyunTokenResponse.class);
            if (response == null || !response.isSuccess()) {
                String code = response == null ? "null" : String.valueOf(response.getCode());
                String message = response == null ? "响应为空" : response.getMessage();
                log.info("【{}】{}终止, code={}, message={}, timestamp={}",
                        KeruyunConstants.LOG_BIZ, ACTION, code, message, timestamp);
                throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED,
                        "客如云获取 token 失败: code=" + code + ", message=" + message);
            }
            log.info("【{}】{}成功, messageUuid={}, timestamp={}",
                    KeruyunConstants.LOG_BIZ, ACTION, response.getMessageUuid(), timestamp);
            return response;
        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", KeruyunConstants.LOG_BIZ, ACTION, e.getMessage());
            log.error("【{}】{} HTTP 调用失败, timestamp={}", KeruyunConstants.LOG_BIZ, ACTION, timestamp, e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "客如云获取 token 超时或网络异常", e);
        }
    }

    /**
     * 拼接完整 URL：公共参数 + sign 均走 query，sign 不参与签名串
     */
    private String buildUrl(Map<String, String> publicParams, String sign) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(keruyunProperties.getBaseUrl() + KeruyunConstants.PATH_GET_TOKEN);
        publicParams.forEach(builder::queryParam);
        builder.queryParam(KeruyunConstants.PARAM_SIGN, sign);
        return builder.build().encode().toUriString();
    }
}
