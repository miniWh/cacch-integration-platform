package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignResponse;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * Moka 入离职 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #resignApplication(Integer, MokaApplicationResignRequest)} — 标记候选人已离职
 *         （PUT /api-platform/v1/applications/{applicationId}/resign）</li>
 * </ul>
 *
 * <p>鉴权：HTTP Basic Auth，将机构 API Key 作为 username（password 为空），
 * 即 {@code Authorization: Basic Base64(apiKey + ":")}。API Key 经配置属性
 * {@link MokaProperties#getApiKey()} 注入，严禁记入日志。</p>
 *
 * <p>网关地址取自 {@link MokaProperties#getBaseUrl()}（yml {@code moka.base-url}），
 * 与 {@link MokaConstants} 中的路径常量在运行时拼接；代码中不硬编码任何环境地址，
 * 测试 / 生产域名切换只改配置，无需重新打包。</p>
 *
 * <p>注意：listApplications（申请分页查询）仍在 {@link MokaOrgClient} 中，
 * 待 MokaPerson 业务域调整时再统一迁入。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaApplicationClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_RESIGN_APPLICATION = "标记候选人已离职";

    private final RestTemplate restTemplate;
    private final MokaProperties mokaProperties;

    /**
     * 标记候选人已离职 —— 调用 Moka {@code PUT /api-platform/v1/applications/{applicationId}/resign}
     *
     * <p>将指定申请归档为已离职。请求体中除 {@code detail}（离职原因）必填外，
     * 其余字段均可选；{@code isResignWriteJob=1} 时 {@code resignJobName} 必填
     * （该条件校验由上层负责，Client 不重复校验）。</p>
     *
     * <p>响应成功判定使用 {@code code} 字段（Moka 官方已废弃 success 字段，
     * 严禁用于业务判断）；成功码兼容 {"0", "200", "success"}。</p>
     *
     * @param applicationId 候选人申请 id（必填，大于 0），来自 Moka 申请查询接口
     * @param request       离职请求体（必填，detail 必填）
     * @return Moka 离职操作响应（code + errorMessage）
     * @throws RestClientException API Key 未配置、参数非法、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public MokaApplicationResignResponse resignApplication(Integer applicationId, MokaApplicationResignRequest request) {
        // —— 前置守卫：API Key 必须已配置 ——
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_RESIGN_APPLICATION);
            throw new RestClientException("Moka API Key 未配置");
        }

        // —— 前置守卫：申请 id 与请求体非空 ——
        if (applicationId == null || applicationId <= 0) {
            log.info("【{}】{}终止, reason=applicationId 非法, applicationId={}", BIZ, ACTION_RESIGN_APPLICATION, applicationId);
            throw new RestClientException("Moka 标记候选人已离职 applicationId 非法: " + applicationId);
        }
        if (request == null) {
            log.info("【{}】{}终止, reason=请求体为空", BIZ, ACTION_RESIGN_APPLICATION);
            throw new RestClientException("Moka 标记候选人已离职请求体为空");
        }

        // —— 拼接 URL（替换路径占位符）——
        String url = mokaProperties.getBaseUrl()
                + MokaConstants.APPLICATION_RESIGN_PATH
                .replace(MokaConstants.APPLICATION_ID_PLACEHOLDER, String.valueOf(applicationId));
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            log.info("【{}】{}终止, reason=URL 非法, url={}", BIZ, ACTION_RESIGN_APPLICATION, url);
            throw new RestClientException("Moka 标记候选人已离职 URL 非法: " + url, e);
        }

        // —— 构造 Basic Auth Header（apiKey 作为 username，password 为空）——
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((mokaProperties.getApiKey() + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth);

        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION_RESIGN_APPLICATION, uri.toString(), request);

        try {
            HttpEntity<MokaApplicationResignRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<MokaApplicationResignResponse> response = restTemplate.exchange(
                    uri, HttpMethod.PUT, entity, MokaApplicationResignResponse.class);
            MokaApplicationResignResponse body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION_RESIGN_APPLICATION, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空, applicationId={}", BIZ, ACTION_RESIGN_APPLICATION, applicationId);
                throw new RestClientException("Moka 标记候选人已离职响应为空");
            }

            if (body.isSuccess()) {
                log.info("【{}】{}成功, applicationId={}", BIZ, ACTION_RESIGN_APPLICATION, applicationId);
            } else {
                log.info("【{}】{}终止, applicationId={}, code={}, errorMessage={}",
                        BIZ, ACTION_RESIGN_APPLICATION, applicationId, body.getCode(), body.getErrorMessage());
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}, applicationId={}", BIZ, ACTION_RESIGN_APPLICATION, e.getMessage(), applicationId);
            log.error("【{}】{} HTTP 调用失败, applicationId={}", BIZ, ACTION_RESIGN_APPLICATION, applicationId, e);
            throw e;
        }
    }
}
