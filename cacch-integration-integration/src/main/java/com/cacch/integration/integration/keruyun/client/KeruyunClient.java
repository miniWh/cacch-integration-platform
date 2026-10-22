package com.cacch.integration.integration.keruyun.client;

import com.cacch.integration.common.config.keruyun.KeruyunProperties;
import com.cacch.integration.common.constant.keruyun.KeruyunCommonConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.keruyun.support.KeruyunSignSupport;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * 客如云通用请求客户端
 *
 * <p>负责公共参数拼装、V2 签名与 URL 组装，HTTP 调用与出入参日志由
 * {@link RestTemplateHttpClient} 统一承担。token 由调用方（Manager）从
 * Token Service 获取后传入，本类不感知缓存。</p>
 *
 * <p>签名所用请求体即实际发送的 JSON 字符串（经 {@link KeruyunSignSupport#compactJson}
 * 压缩），确保签名与报文逐字符一致。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeruyunClient {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final RestTemplateHttpClient httpClient;
    private final KeruyunProperties keruyunProperties;
    private final KeruyunSignSupport keruyunSignSupport;

    /**
     * 发送客如云 POST 业务请求
     *
     * @param path         接口路径，如 {@code /open/v1/order/create}，不可为空
     * @param body         业务请求体对象，可为 null（无请求体时不参与签名）
     * @param token        门店 token，由 Token Service 提供，不可为空
     * @param responseType 响应 DTO 类型
     * @param <T>          响应 DTO 类型
     * @return 反序列化后的业务响应
     * @throws BizException 配置缺失、token 为空、序列化失败、网络异常时抛出
     */
    public <T> T post(String path, Object body, String token, Class<T> responseType) {
        return execute(HttpMethod.POST, path, body, null, token, responseType);
    }

    /**
     * 发送客如云 GET 业务请求
     *
     * @param path         接口路径，不可为空
     * @param query        业务查询参数，可为 null
     * @param token        门店 token，由 Token Service 提供，不可为空
     * @param responseType 响应 DTO 类型
     * @param <T>          响应 DTO 类型
     * @return 反序列化后的业务响应
     * @throws BizException 配置缺失、token 为空、网络异常时抛出
     */
    public <T> T get(String path, Map<String, String> query, String token, Class<T> responseType) {
        return execute(HttpMethod.GET, path, null, query, token, responseType);
    }

    /**
     * 公共执行模板：校验 → 序列化 body → 签名 → 组装 URL → 调用
     */
    private <T> T execute(HttpMethod method, String path, Object body, Map<String, String> query,
                          String token, Class<T> responseType) {
        String action = "请求" + path;
        validate(path, token, action);

        String compactBody = serializeBody(body, action);
        String timestamp = keruyunSignSupport.currentTimestampSeconds();
        Map<String, String> publicParams = keruyunSignSupport.buildPublicParams(timestamp);
        String sign = keruyunSignSupport.signForRequest(compactBody, publicParams, token);
        String url = buildUrl(path, query, publicParams, sign);

        log.debug("【客如云签名】最终请求, method={}, path={}, timestamp={}, 公共参数={}, sign={}, url={}",
                method, path, timestamp, publicParams, sign, url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            if (HttpMethod.GET.equals(method)) {
                return httpClient.doGet(url, headers, KeruyunCommonConstants.LOG_BIZ, action, responseType);
            }
            return httpClient.doPostOrPut(method, url, compactBody, headers,
                    KeruyunCommonConstants.LOG_BIZ, action, responseType);
        } catch (RestClientException e) {
            log.error("【{}】{} HTTP 调用失败, path={}", KeruyunCommonConstants.LOG_BIZ, action, path, e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "客如云接口调用超时或网络异常: " + path, e);
        }
    }

    /**
     * 前置校验：配置、路径与 token
     */
    private void validate(String path, String token, String action) {
        if (!StringUtils.hasText(path)) {
            log.info("【{}】{}终止, reason=接口路径为空", KeruyunCommonConstants.LOG_BIZ, action);
            throw new BizException(ResultCode.PARAM_MISSING, "客如云接口路径不能为空");
        }
        if (keruyunProperties.isAuthConfigured()) {
            log.info("【{}】{}终止, reason=鉴权配置不完整", KeruyunCommonConstants.LOG_BIZ, action);
            throw new BizException(ResultCode.PARAM_INVALID, "客如云鉴权配置不完整，无法发起业务请求");
        }
        if (!StringUtils.hasText(token)) {
            log.info("【{}】{}终止, reason=token 为空", KeruyunCommonConstants.LOG_BIZ, action);
            throw new BizException(ResultCode.INTEGRATION_AUTH_FAILED, "客如云门店 token 为空，无法发起业务请求");
        }
    }

    /**
     * 序列化请求体并压缩空白，返回值同时用于签名与实际发送
     */
    private String serializeBody(Object body, String action) {
        if (body == null) {
            return null;
        }
        try {
            return KeruyunSignSupport.compactJson(OBJECT_MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            log.error("【{}】{} 请求体序列化失败", KeruyunCommonConstants.LOG_BIZ, action, e);
            throw new BizException(ResultCode.PARAM_INVALID, "客如云请求体序列化失败", e);
        }
    }

    /**
     * 拼接完整 URL：业务查询参数 + 公共参数 + sign
     *
     * <p><strong>严禁将 token 作为 URL 参数传递</strong>：客如云服务端复算签名时会把 URL 上
     * 收到的全部 query 参数一并纳入签名原文，多传 token（或任何未参与签名计算的参数）都会导致
     * 服务端算出的 sign 与本地不一致，返回 {@code code=1001 签名错误}。token 只参与签名计算，
     * 不出现在请求中（服务端按 appKey + shopIdenty 自行关联 token）。</p>
     *
     * @param path         接口路径
     * @param query        业务查询参数，可为 null
     * @param publicParams 公共参数（appKey / shopIdenty / timestamp / version）
     * @param sign         签名
     * @return 完整请求 URL
     */
    private String buildUrl(String path, Map<String, String> query, Map<String, String> publicParams,
                            String sign) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(keruyunProperties.getBaseUrl() + path);
        if (query != null && !query.isEmpty()) {
            query.forEach(builder::queryParam);
        }
        publicParams.forEach(builder::queryParam);
        builder.queryParam(KeruyunCommonConstants.PARAM_SIGN, sign);
        return builder.build().encode().toUriString();
    }
}
