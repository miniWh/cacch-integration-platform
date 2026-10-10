package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * Moka 入离职 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担，本类仅保留 URL 拼接、
 * Basic Auth 构造、方法级前置校验、成功 INFO。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaApplicationClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_RESIGN_APPLICATION = "标记候选人已离职";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 标记候选人已离职
     */
    public MokaApplicationResignResponse resignApplication(Integer applicationId, MokaApplicationResignRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_RESIGN_APPLICATION);
            throw new RestClientException("Moka API Key 未配置");
        }
        if (applicationId == null || applicationId <= 0) {
            log.info("【{}】{}终止, reason=applicationId 非法, applicationId={}",
                    BIZ, ACTION_RESIGN_APPLICATION, applicationId);
            throw new RestClientException("Moka 标记候选人已离职 applicationId 非法: " + applicationId);
        }
        if (request == null) {
            log.info("【{}】{}终止, reason=请求体为空", BIZ, ACTION_RESIGN_APPLICATION);
            throw new RestClientException("Moka 标记候选人已离职请求体为空");
        }

        String url = mokaProperties.getBaseUrl()
                + MokaConstants.APPLICATION_RESIGN_PATH
                .replace(MokaConstants.APPLICATION_ID_PLACEHOLDER, String.valueOf(applicationId));
        HttpHeaders headers = buildBasicAuthHeaders(true);

        MokaApplicationResignResponse body = httpClient.doPostOrPut(
                HttpMethod.PUT, url, request, headers, BIZ, ACTION_RESIGN_APPLICATION, MokaApplicationResignResponse.class);

        if (body.isSuccess()) {
            log.info("【{}】{}成功, applicationId={}", BIZ, ACTION_RESIGN_APPLICATION, applicationId);
        } else {
            log.info("【{}】{}终止, applicationId={}, code={}, errorMessage={}",
                    BIZ, ACTION_RESIGN_APPLICATION, applicationId, body.getCode(), body.getErrorMessage());
        }
        return body;
    }

    private HttpHeaders buildBasicAuthHeaders(boolean includeContentType) {
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((mokaProperties.getApiKey() + ":").getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        if (includeContentType) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth);
        return headers;
    }
}
