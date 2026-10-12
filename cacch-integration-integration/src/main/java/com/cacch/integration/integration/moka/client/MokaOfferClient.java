package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoRequest;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoResponse;
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
 * Moka offer 扩展信息 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担，本类仅保留 URL 拼接、
 * Basic Auth 构造、方法级前置校验、成功 INFO。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaOfferClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_GET_OFFER_EXTEND_INFO = "获取offer扩展信息";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 获取 offer 扩展信息
     *
     * <p>对接 Moka API {@code POST /api-platform/v3/getOfferExtendInfos}，
     * 按申请 ID 列表查询 offer 薪资、入职日期、状态、自定义字段、附件、发送历史等。</p>
     *
     * @param request 请求体（orgId + applicationIds + offerAttachment），
     *                orgId 由 Service 层从配置注入，不可为空
     * @return Moka offer 扩展信息响应（含 code/msg/successField/data）
     * @throws RestClientException API Key 未配置、请求体为空、URL 非法、HTTP 调用失败
     *                             或响应体为空时抛出
     */
    public MokaOfferExtendInfoResponse getOfferExtendInfo(MokaOfferExtendInfoRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_GET_OFFER_EXTEND_INFO);
            throw new RestClientException("Moka API Key 未配置");
        }
        if (request == null) {
            log.info("【{}】{}终止, reason=请求体为空", BIZ, ACTION_GET_OFFER_EXTEND_INFO);
            throw new RestClientException("Moka 获取 offer 扩展信息请求体为空");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.OFFER_EXTEND_INFO_PATH;
        HttpHeaders headers = buildBasicAuthHeaders(true);

        MokaOfferExtendInfoResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ,
                ACTION_GET_OFFER_EXTEND_INFO, MokaOfferExtendInfoResponse.class);

        if (body.isSuccess()) {
            int dataSize = body.getData() != null ? body.getData().size() : 0;
            log.info("【{}】{}成功, applicationIds={}, 返回记录数={}",
                    BIZ, ACTION_GET_OFFER_EXTEND_INFO,
                    request.getApplicationIds(), dataSize);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_GET_OFFER_EXTEND_INFO, body.getCode(), body.getMsg());
        }
        return body;
    }

    /**
     * 构造 Basic Auth 鉴权头（API Key 作为 username，password 为空）
     *
     * @param includeContentType 是否包含 Content-Type: application/json
     * @return 构造完成的 HttpHeaders
     */
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
