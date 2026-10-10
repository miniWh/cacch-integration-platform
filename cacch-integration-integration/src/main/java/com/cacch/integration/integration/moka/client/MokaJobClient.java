package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaJobListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaJobListResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * Moka 职位 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担，本类仅保留 URL 拼接、
 * Basic Auth 构造、成功 INFO。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaJobClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_GET_JOBS = "查询职位信息";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 查询职位信息 —— 调用 Moka {@code POST /api-platform/v1/jobs/getJobs}
     *
     * <p>入参/出参完整报文由 {@link RestTemplateHttpClient} 经 {@code ThirdPartyHttpLogSupport}
     * 脱敏后统一打印，本方法仅输出业务摘要日志。</p>
     *
     * @param request 查询条件；hireMode 必填（social=社招 / campus=校招），其余字段可选
     * @return Moka 响应（含 total 职位总数与 jobs 职位列表）；业务失败时仍返回原始响应，
     * 由上层根据 isSuccess 判定
     * @throws IllegalStateException                              Moka API Key 未配置时抛出
     * @throws org.springframework.web.client.RestClientException HTTP 调用失败时抛出
     */
    public MokaJobListResponse getJobs(MokaJobListRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_GET_JOBS);
            throw new IllegalStateException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.JOB_LIST_PATH;
        HttpHeaders headers = buildBasicAuthHeaders(true);

        MokaJobListResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ, ACTION_GET_JOBS, MokaJobListResponse.class);

        if (body.isSuccess()) {
            int jobSize = body.getData() == null || body.getData().getJobs() == null
                    ? 0 : body.getData().getJobs().size();
            Integer total = body.getData() == null ? null : body.getData().getTotal();
            log.info("【{}】{}成功, total={}, jobSize={}", BIZ, ACTION_GET_JOBS, total, jobSize);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_GET_JOBS, body.getCode(), body.getMsg());
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
