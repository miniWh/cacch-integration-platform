package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaDeptListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * Moka 组织架构 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #syncDepartmentsFull(MokaDeptSyncRequest)} — 组织架构全量同步（PUT /api-platform/v2/departments）</li>
 *     <li>{@link #getDepartments(String)} — 获取全量组织架构（GET /api-platform/v1/departments）</li>
 * </ul>
 *
 * <p>HTTP 通用调用（URI 构造、出入参日志、异常兜底）统一由
 * {@link RestTemplateHttpClient} 承担，本类仅保留 URL 拼接、Basic Auth 构造、
 * 方法级前置校验、成功 INFO。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaOrgClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_FULL_SYNC = "组织架构全量同步";
    private static final String ACTION_GET_DEPTS = "获取全量组织架构";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 组织架构全量同步
     *
     * @param request 同步请求体（departments 列表必填）
     * @return Moka 同步结果响应
     * @throws RestClientException API Key 未配置、请求体非法、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public MokaDeptSyncResponse syncDepartmentsFull(MokaDeptSyncRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_FULL_SYNC);
            throw new RestClientException("Moka API Key 未配置");
        }
        if (request == null || request.getDepartments() == null) {
            log.info("【{}】{}终止, reason=请求体或 departments 为空", BIZ, ACTION_FULL_SYNC);
            throw new RestClientException("Moka 组织架构全量同步请求体为空");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.DEPT_FULL_SYNC_PATH;
        log.info("【{}】开始{}, deptCount={}", BIZ, ACTION_FULL_SYNC, request.getDepartments().size());

        HttpHeaders headers = buildBasicAuthHeaders(true);
        MokaDeptSyncResponse body = httpClient.doPostOrPut(
                HttpMethod.PUT, url, request, headers, BIZ, ACTION_FULL_SYNC, MokaDeptSyncResponse.class);

        if (body.isSuccess()) {
            log.info("【{}】{}成功, new={}, update={}, delete={}",
                    BIZ, ACTION_FULL_SYNC, body.getNewCount(), body.getUpdateCount(), body.getDeleteCount());
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_FULL_SYNC, body.getCode(), body.getMsg());
        }
        return body;
    }

    /**
     * 获取全量组织架构
     *
     * @param updateTimeStart 增量查询起始时间（可选，格式 yyyy-MM-dd HH:mm:ss）
     * @return Moka 全量组织架构响应
     * @throws RestClientException API Key 未配置、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public MokaDeptListResponse getDepartments(String updateTimeStart) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_GET_DEPTS);
            throw new RestClientException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.DEPT_LIST_PATH;
        if (StringUtils.hasText(updateTimeStart)) {
            url += "?updateTimeStart=" + updateTimeStart;
        }

        HttpHeaders headers = buildBasicAuthHeaders(false);
        MokaDeptListResponse body = httpClient.doGet(url, headers, BIZ, ACTION_GET_DEPTS, MokaDeptListResponse.class);

        if (body.isSuccess()) {
            int deptCount = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, deptCount={}", BIZ, ACTION_GET_DEPTS, deptCount);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_GET_DEPTS, body.getCode(), body.getMsg());
        }
        return body;
    }

    /**
     * 构造 Moka Basic Auth Headers
     *
     * @param includeContentType true → 加 Content-Type: application/json（POST/PUT）；false → 不加（GET）
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
