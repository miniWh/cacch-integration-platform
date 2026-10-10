package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
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
 * Moka HTTP 通用调用器 —— 封装 Basic Auth、URL 构造、出入参日志、异常兜底
 *
 * <p>所有 Moka 域 Client（Org / Candidates / Application / User / Role）共享本类，
 * 消除每个接口方法中重复出现的 ~30 行模板代码（API Key 守卫、Basic Auth 组装、
 * URI 构造、RestTemplate 调用、ThirdPartyHttpLogSupport 日志、null body 检查、
 * RestClientException catch + rethrow）。</p>
 *
 * <p>本类仅做<strong>通用模板</strong>：
 * <ul>
 *     <li>✅ API Key 未配置守卫 + INFO 终止日志</li>
 *     <li>✅ URI 构造 + URISyntaxException catch + rethrow</li>
 *     <li>✅ Basic Auth Header（Base64(apiKey + ":")）</li>
 *     <li>✅ Content-Type / Accept / Authorization 组装</li>
 *     <li>✅ ThirdPartyHttpLogSupport.logRequest / logResponse 出入参日志</li>
 *     <li>✅ null body 检查 + RestClientException</li>
 *     <li>✅ RestClientException catch → INFO 终止 + ERROR stack + rethrow</li>
 * </ul>
 *
 * <p>本类<strong>不做</strong>（由调用方 Client 负责）：
 * <ul>
 *     <li>❌ URL 拼接（query 参数、路径占位符替换 —— 各接口不同）</li>
 *     <li>❌ 方法级请求体前置校验（如 request.getUsersInfo() 非空）</li>
 *     <li>❌ Moka 业务成功判定（body.isSuccess() / code 判断 —— 各接口成功码不同）</li>
 *     <li>❌ 成功 INFO 日志（各接口统计项不同：deptCount / roleCount / listSize …）</li>
 * </ul></p>
 *
 * <p>鉴权：HTTP Basic Auth，apiKey 作为 username，password 为空。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaHttpClient {

    private final RestTemplate restTemplate;
    private final MokaProperties mokaProperties;

    /**
     * 发送 HTTP GET 请求 —— 无请求体
     *
     * @param fullUrl      完整 URL（调用方负责拼接 baseUrl + path + query 参数）
     * @param biz          业务标识（ThirdPartyHttpLogSupport 用，如 "Moka"）
     * @param action       操作标识（日志用，如 "获取全量角色"）
     * @param responseType 响应体类型 Class
     * @param <T>          响应体类型
     * @return 解析后的响应体；不会返回 null（为空时抛 RestClientException）
     * @throws RestClientException API Key 未配置、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public <T> T doGet(String fullUrl, String biz, String action, Class<T> responseType) {
        // —— 通用守卫：API Key 必须已配置 ——
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", biz, action);
            throw new RestClientException("Moka API Key 未配置");
        }

        URI uri = buildUri(fullUrl, biz, action);
        HttpHeaders headers = buildHeaders(false);
        ThirdPartyHttpLogSupport.logRequest(biz, action, uri.toString(), null);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, entity, responseType);
            T body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(biz, action, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", biz, action);
                throw new RestClientException(biz + " " + action + " 响应为空");
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", biz, action, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", biz, action, e);
            throw e;
        }
    }

    /**
     * 发送 HTTP POST / PUT 请求 —— 带 JSON 请求体
     *
     * @param method       HttpMethod.POST 或 HttpMethod.PUT
     * @param fullUrl      完整 URL（调用方负责拼接 baseUrl + path + 路径占位符替换）
     * @param requestBody  请求体对象（可为 null）
     * @param biz          业务标识
     * @param action       操作标识
     * @param responseType 响应体类型 Class
     * @param <T>          响应体类型
     * @return 解析后的响应体；不会返回 null
     * @throws RestClientException 同上
     */
    public <T> T doPostOrPut(HttpMethod method, String fullUrl, Object requestBody,
                             String biz, String action, Class<T> responseType) {
        // —— 通用守卫：API Key 必须已配置 ——
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", biz, action);
            throw new RestClientException("Moka API Key 未配置");
        }

        URI uri = buildUri(fullUrl, biz, action);
        HttpHeaders headers = buildHeaders(true);
        ThirdPartyHttpLogSupport.logRequest(biz, action, uri.toString(), requestBody);

        try {
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, responseType);
            T body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(biz, action, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", biz, action);
                throw new RestClientException(biz + " " + action + " 响应为空");
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", biz, action, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", biz, action, e);
            throw e;
        }
    }

    /**
     * 将字符串 URL 构造为 URI，失败时包装为 RestClientException
     */
    private URI buildUri(String fullUrl, String biz, String action) {
        try {
            return new URI(fullUrl);
        } catch (URISyntaxException e) {
            log.info("【{}】{}终止, reason=URL 非法, url={}", biz, action, fullUrl);
            throw new RestClientException(biz + " " + action + " URL 非法: " + fullUrl, e);
        }
    }

    /**
     * 构造通用 HTTP Header（Basic Auth + Accept）
     *
     * @param includeContentType true → 额外加 Content-Type: application/json（POST/PUT 场景）；
     *                           false → 不加（GET 场景）
     */
    private HttpHeaders buildHeaders(boolean includeContentType) {
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
