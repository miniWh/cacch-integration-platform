package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse;
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
 * Moka 候选人 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #listApplications(MokaApplicationListRequest)} — 根据条件查询申请信息
 *         （POST /api-platform/v3/applications/list_by_condition，成功码 code=200）</li>
 * </ul>
 *
 * <p>鉴权：HTTP Basic Auth，将机构 API Key 作为 username（password 为空），
 * 即 {@code Authorization: Basic Base64(apiKey + ":")}。API Key 经配置属性
 * {@link MokaProperties#getApiKey()} 注入，严禁记入日志。</p>
 *
 * <p>网关地址取自 {@link MokaProperties#getBaseUrl()}（yml {@code moka.base-url}），
 * 与 {@link MokaConstants} 中的路径常量在运行时拼接。</p>
 *
 * <p>历史：listApplications 原在 {@link MokaOrgClient}，2026-10 迁入本类，
 * 作为候选人域（Candidates）的唯一 Client。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaCandidatesClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_LIST_APPLICATIONS = "根据条件查询申请信息";

    private final RestTemplate restTemplate;
    private final MokaProperties mokaProperties;

    /**
     * 根据条件查询申请信息（分页）—— 调用 Moka {@code POST /api-platform/v3/applications/list_by_condition}
     *
     * <p>支持按申请 id、候选人 id、职位 id、流程、阶段、招聘模式、归档状态、时间范围等条件查询。
     * 分页通过 {@code limit}（默认 20，最大 20）和 {@code next}（游标）实现。</p>
     *
     * <p>注意：成功码为 200（与部门接口的 code=0 不同）。</p>
     *
     * @param request 查询条件（所有字段均可选，空对象表示返回全部）
     * @return 申请分页响应（含 applicationList + next 分页游标）
     * @throws RestClientException API Key 未配置、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public MokaApplicationListResponse listApplications(MokaApplicationListRequest request) {
        // —— 前置守卫：API Key 必须已配置 ——
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_LIST_APPLICATIONS);
            throw new RestClientException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.APPLICATION_LIST_PATH;
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            log.info("【{}】{}终止, reason=URL 非法, url={}", BIZ, ACTION_LIST_APPLICATIONS, url);
            throw new RestClientException("申请查询 URL 非法: " + url, e);
        }

        // —— 构造 Basic Auth Header ——
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((mokaProperties.getApiKey() + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth);

        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION_LIST_APPLICATIONS, uri.toString(), request);

        try {
            HttpEntity<MokaApplicationListRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<MokaApplicationListResponse> response = restTemplate.exchange(
                    uri, HttpMethod.POST, entity, MokaApplicationListResponse.class);
            MokaApplicationListResponse body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION_LIST_APPLICATIONS, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, ACTION_LIST_APPLICATIONS);
                throw new RestClientException("申请查询响应为空");
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

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, ACTION_LIST_APPLICATIONS, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, ACTION_LIST_APPLICATIONS, e);
            throw e;
        }
    }
}
