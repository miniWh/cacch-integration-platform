package com.cacch.integration.integration.support;

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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Objects;

/**
 * 通用 RestTemplate HTTP 调用器 —— 封装 URI 构造、出入参日志、异常兜底
 *
 * <p>所有业务域 Client（Moka / iHR / WeCom / FDD / CRM / OA）共享本类，
 * 消除每个接口方法中重复出现的 ~30 行模板代码（URI 构造 +
 * ThirdPartyHttpLogSupport 出入参日志 + RestTemplate.exchange + null body 检查 +
 * RestClientException catch + rethrow）。</p>
 *
 * <p>鉴权头（Basic Auth / Bearer Token / URL 内嵌 token / 签名 Header 等）因各业务域
 * 差异较大，由调用方 Client 自行构造后传入；本类不做任何鉴权逻辑。</p>
 *
 * <p>本类仅做<strong>通用模板</strong>：
 * <ul>
 *     <li>✅ URI 构造 + URISyntaxException catch + rethrow</li>
 *     <li>✅ ThirdPartyHttpLogSupport.logRequest / logResponse 出入参日志</li>
 *     <li>✅ RestTemplate.exchange 调用</li>
 *     <li>✅ null body 检查 + RestClientException</li>
 *     <li>✅ RestClientException catch → INFO 终止 + ERROR stack + rethrow</li>
 * </ul>
 *
 * <p>本类<strong>不做</strong>（由调用方 Client 负责）：
 * <ul>
 *     <li>❌ URL 拼接（query 参数、路径占位符替换 —— 各接口不同）</li>
 *     <li>❌ 鉴权头构造（各业务域完全不同）</li>
 *     <li>❌ 方法级请求体前置校验（如 request.getDepartments() 非空）</li>
 *     <li>❌ 业务成功判定（各接口成功码不同）</li>
 *     <li>❌ 成功 INFO 日志（各接口统计项不同）</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestTemplateHttpClient {

    /**
     * 静态 ObjectMapper —— 用于将原始响应报文反序列化为目标类型
     *
     * <p>与 {@link ThirdPartyHttpLogSupport} 使用相同模式（静态 JsonMapper），
     * 不依赖 Spring Bean 注入，避免 Bean 缺失导致启动失败。
     * Jackson 3.x 默认注册 JavaTimeModule，可处理 LocalDateTime 等类型。</p>
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final RestTemplate restTemplate;

    /**
     * 发送 HTTP GET 请求 —— 无请求体
     *
     * @param fullUrl      完整 URL（调用方负责拼接 baseUrl + path + query 参数）
     * @param headers      HTTP Headers（调用方负责构造鉴权头，如 Basic Auth / Bearer / 签名）
     * @param biz          业务标识（ThirdPartyHttpLogSupport 用，如 "Moka"）
     * @param action       操作标识（日志用，如 "获取全量角色"）
     * @param responseType 响应体类型 Class
     * @param <T>          响应体类型
     * @return 解析后的响应体；不会返回 null（为空时抛 RestClientException）
     * @throws RestClientException URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public <T> T doGet(String fullUrl, HttpHeaders headers, String biz, String action, Class<T> responseType) {
        return executeInternal(restTemplate, HttpMethod.GET, fullUrl, null, headers, biz, action, responseType);
    }

    /**
     * 发送 HTTP POST / PUT 请求 —— 带 JSON 请求体
     *
     * @param method       HttpMethod.POST 或 HttpMethod.PUT
     * @param fullUrl      完整 URL（调用方负责拼接 baseUrl + path + 路径占位符替换）
     * @param requestBody  请求体对象（可为 null）
     * @param headers      HTTP Headers（调用方负责构造鉴权头 + Content-Type）
     * @param biz          业务标识
     * @param action       操作标识
     * @param responseType 响应体类型 Class
     * @param <T>          响应体类型
     * @return 解析后的响应体；不会返回 null
     * @throws RestClientException 同上
     */
    public <T> T doPostOrPut(HttpMethod method, String fullUrl, Object requestBody,
                             HttpHeaders headers, String biz, String action, Class<T> responseType) {
        return executeInternal(restTemplate, method, fullUrl, requestBody, headers, biz, action, responseType);
    }

    /**
     * 内部通用执行模板
     */
    private <T> T executeInternal(RestTemplate rt, HttpMethod method, String fullUrl,
                                  Object requestBody, HttpHeaders headers, String biz, String action, Class<T> responseType) {
        URI uri = buildUri(fullUrl, biz, action);

        // 补全默认 Accept（若调用方未设置）
        HttpHeaders effectiveHeaders = headers != null ? headers : new HttpHeaders();
        if (effectiveHeaders.get(HttpHeaders.ACCEPT) == null || Objects.requireNonNull(effectiveHeaders.get(HttpHeaders.ACCEPT)).isEmpty()) {
            effectiveHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        }

        ThirdPartyHttpLogSupport.logRequest(biz, action, uri.toString(), requestBody);

        try {
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, effectiveHeaders);

            // 先以 String.class 交换，获取原始响应报文（确保日志包含 DTO 未映射的字段）
            ResponseEntity<String> rawResponse = rt.exchange(uri, method, entity, String.class);
            String rawBody = rawResponse.getBody();

            // 打印原始响应（脱敏由 ThirdPartyHttpLogSupport 处理）
            ThirdPartyHttpLogSupport.logResponse(biz, action, rawBody);

            // null body 检查（语义不变）
            if (rawBody == null) {
                log.info("【{}】{}终止, reason=响应体为空", biz, action);
                throw new RestClientException(biz + " " + action + " 响应为空");
            }

            // responseType 为 String.class 时直接返回原始报文
            if (responseType == String.class) {
                return (T) rawBody;
            }

            // 反序列化为目标类型
            try {
                return MAPPER.readValue(rawBody, responseType);
            } catch (Exception e) {
                log.info("【{}】{}终止, reason=响应体反序列化失败, responseType={}", biz, action, responseType.getSimpleName());
                log.error("【{}】{} 响应体反序列化失败, responseType={}", biz, action, responseType.getSimpleName(), e);
                throw new RestClientException(
                        biz + " " + action + " 响应体反序列化失败: " + e.getMessage(), e);
            }

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
}
