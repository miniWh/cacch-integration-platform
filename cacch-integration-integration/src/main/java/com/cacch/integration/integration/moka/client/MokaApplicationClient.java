package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationHiredRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationHiredResponse;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationRejectedRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationRejectedResponse;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

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
    private static final String ACTION_REJECT_APPLICATION = "标记候选人未入职";
    private static final String ACTION_HIRE_APPLICATION = "标记候选人已入职";

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
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

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

    /**
     * 标记候选人未入职（归档申请并放入人才库）
     *
     * <p>对接 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/rejected}。
     * 要求申请须处于「待入职」阶段且不在人才库中，否则 Moka 返回失败。</p>
     *
     * @param applicationId 候选人申请 id，对应路径变量 {@code applicationId}；不可为 null 且须大于 0
     * @param request       请求体，字段均可选；不传 {@code archiveReasonId}/{@code talentPoolId}
     *                      时 Moka 使用内置默认值；不可为 null
     * @return Moka 原始响应体；调用失败时 {@code isSuccess()} 返回 false，错误信息见 {@code errorMessage}
     * @throws RestClientException Moka API Key 未配置、applicationId 非法、请求体为空，或 HTTP 调用异常时抛出
     */
    public MokaApplicationRejectedResponse rejectApplication(Integer applicationId, MokaApplicationRejectedRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_REJECT_APPLICATION);
            throw new RestClientException("Moka API Key 未配置");
        }
        if (applicationId == null || applicationId <= 0) {
            log.info("【{}】{}终止, reason=applicationId 非法, applicationId={}",
                    BIZ, ACTION_REJECT_APPLICATION, applicationId);
            throw new RestClientException("Moka 标记候选人未入职 applicationId 非法: " + applicationId);
        }
        if (request == null) {
            log.info("【{}】{}终止, reason=请求体为空", BIZ, ACTION_REJECT_APPLICATION);
            throw new RestClientException("Moka 标记候选人未入职请求体为空");
        }

        String url = mokaProperties.getBaseUrl()
                + MokaConstants.APPLICATION_REJECTED_PATH
                .replace(MokaConstants.APPLICATION_ID_PLACEHOLDER, String.valueOf(applicationId));
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

        MokaApplicationRejectedResponse body = httpClient.doPostOrPut(
                HttpMethod.PUT, url, request, headers, BIZ, ACTION_REJECT_APPLICATION, MokaApplicationRejectedResponse.class);

        if (body.isSuccess()) {
            log.info("【{}】{}成功, applicationId={}", BIZ, ACTION_REJECT_APPLICATION, applicationId);
        } else {
            log.info("【{}】{}终止, applicationId={}, errorMessage={}",
                    BIZ, ACTION_REJECT_APPLICATION, applicationId, body.getErrorMessage());
        }
        return body;
    }

    /**
     * 标记候选人已入职
     *
     * <p>对接 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/hired}。
     * 请求体中 {@code hcId} 为 null 时不会输出到 JSON（不传给 Moka），符合 Moka
     * 「不允许传 hcId="" 或 null」的约束；{@code hiredAt}、{@code probation} 为必填，
     * 由调用方保证（Service 层已校验）。</p>
     *
     * @param applicationId 候选人申请 id，对应路径变量 {@code applicationId}；不可为 null 且须大于 0
     * @param request       入职请求体，{@code hiredAt}/{@code probation} 必填；不可为 null
     * @return Moka 原始响应体；调用失败时 {@code isSuccess()} 返回 false，错误信息见 {@code errorMessage}
     * @throws RestClientException Moka API Key 未配置、applicationId 非法、请求体为空，或 HTTP 调用异常时抛出
     */
    public MokaApplicationHiredResponse hireApplication(Integer applicationId, MokaApplicationHiredRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_HIRE_APPLICATION);
            throw new RestClientException("Moka API Key 未配置");
        }
        if (applicationId == null || applicationId <= 0) {
            log.info("【{}】{}终止, reason=applicationId 非法, applicationId={}",
                    BIZ, ACTION_HIRE_APPLICATION, applicationId);
            throw new RestClientException("Moka 标记候选人已入职 applicationId 非法: " + applicationId);
        }
        if (request == null) {
            log.info("【{}】{}终止, reason=请求体为空", BIZ, ACTION_HIRE_APPLICATION);
            throw new RestClientException("Moka 标记候选人已入职请求体为空");
        }

        String url = mokaProperties.getBaseUrl()
                + MokaConstants.APPLICATION_HIRED_PATH
                .replace(MokaConstants.APPLICATION_ID_PLACEHOLDER, String.valueOf(applicationId));
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

        MokaApplicationHiredResponse body = httpClient.doPostOrPut(
                HttpMethod.PUT, url, request, headers, BIZ, ACTION_HIRE_APPLICATION, MokaApplicationHiredResponse.class);

        if (body.isSuccess()) {
            log.info("【{}】{}成功, applicationId={}", BIZ, ACTION_HIRE_APPLICATION, applicationId);
        } else {
            log.info("【{}】{}终止, applicationId={}, errorMessage={}",
                    BIZ, ACTION_HIRE_APPLICATION, applicationId, body.getErrorMessage());
        }
        return body;
    }
}
