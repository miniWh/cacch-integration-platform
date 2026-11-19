package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaEhrApplicationsV2Request;
import com.cacch.integration.integration.moka.client.dto.MokaEhrApplicationsV2Response;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Moka 候选人 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担，本类仅保留 URL 拼接、
 * Basic Auth 构造、成功 INFO。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaCandidatesClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_LIST_APPLICATIONS = "根据条件查询申请信息";
    private static final String ACTION_EHR_APPLICATIONS_V2 = "获取候选人信息v2";

    /**
     * 静态 ObjectMapper —— 将原始响应报文反序列化为 {@link MokaApplicationListResponse}
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 根据条件查询申请信息（分页）
     *
     * <p>先获取原始 JSON 字符串，在本方法内 {@link ThirdPartyHttpLogSupport#logResponse} 打印原始出参，
     * 再反序列化为响应 DTO。</p>
     *
     * @param request 查询条件与分页参数（含 next 游标）
     * @return Moka 申请列表响应；业务成功与否以 {@link MokaApplicationListResponse#isSuccess()} 判定
     * @throws IllegalStateException Moka API Key 未配置时抛出
     * @throws RestClientException   HTTP 调用失败或响应体反序列化失败时抛出
     */
    public MokaApplicationListResponse listApplications(MokaApplicationListRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_LIST_APPLICATIONS);
            throw new IllegalStateException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.APPLICATION_LIST_PATH;
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

        String rawBody = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ, ACTION_LIST_APPLICATIONS, String.class);

        MokaApplicationListResponse body;
        try {
            body = MAPPER.readValue(rawBody, MokaApplicationListResponse.class);
        } catch (Exception e) {
            log.info("【{}】{}终止, reason=响应体反序列化失败", BIZ, ACTION_LIST_APPLICATIONS);
            log.error("【{}】{} 响应体反序列化失败", BIZ, ACTION_LIST_APPLICATIONS, e);
            throw new RestClientException(BIZ + " " + ACTION_LIST_APPLICATIONS + " 响应体反序列化失败: " + e.getMessage(), e);
        }

        if (body.isSuccess()) {
            int listSize = body.getData() == null || body.getData().getApplicationList() == null
                    ? 0 : body.getData().getApplicationList().size();
            String next = body.getData() == null ? null : body.getData().getNext();
            log.info("【{}】{}成功, listSize={}, next={}", BIZ, ACTION_LIST_APPLICATIONS, listSize, next);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_LIST_APPLICATIONS, body.getCode(), body.getMsg());
        }
        return body;
    }

    /**
     * 获取候选人信息 v2（EHR 申请列表）
     *
     * <p>对接 Moka {@code POST /api-platform/v2/data/ehrApplications}。
     * 先获取原始 JSON，打印出参后再反序列化为响应 DTO。</p>
     *
     * @param request 查询条件与分页参数（含 next 游标）
     * @return Moka EHR 申请响应；成功与否以 {@link MokaEhrApplicationsV2Response#isSuccess()} 判定
     * @throws IllegalStateException Moka API Key 未配置时抛出
     * @throws RestClientException   HTTP 调用失败或响应体反序列化失败时抛出
     */
    public MokaEhrApplicationsV2Response listEhrApplicationsV2(MokaEhrApplicationsV2Request request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_EHR_APPLICATIONS_V2);
            throw new IllegalStateException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.EHR_APPLICATIONS_V2_PATH;
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

        String rawBody = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ, ACTION_EHR_APPLICATIONS_V2, String.class);

        MokaEhrApplicationsV2Response body;
        try {
            body = MAPPER.readValue(rawBody, MokaEhrApplicationsV2Response.class);
        } catch (Exception e) {
            log.info("【{}】{}终止, reason=响应体反序列化失败", BIZ, ACTION_EHR_APPLICATIONS_V2);
            log.error("【{}】{} 响应体反序列化失败", BIZ, ACTION_EHR_APPLICATIONS_V2, e);
            throw new RestClientException(BIZ + " " + ACTION_EHR_APPLICATIONS_V2 + " 响应体反序列化失败: " + e.getMessage(), e);
        }

        if (body.isSuccess()) {
            int listSize = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, listSize={}, next={}", BIZ, ACTION_EHR_APPLICATIONS_V2, listSize, body.getNext());
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_EHR_APPLICATIONS_V2, body.getCode(), body.getMsg());
        }
        return body;
    }
}
