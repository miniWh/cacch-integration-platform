package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * Moka Client 公共鉴权头构造工具
 *
 * <p>Moka 开放平台统一使用 HTTP Basic Auth 鉴权：API Key 作为 username（password 为空），
 * 经 Base64 编码后放入 Authorization 请求头。7 个 Moka Client 均使用相同逻辑，
 * 本类抽离后避免重复代码。</p>
 *
 * <p>仅 Moka 域 Client 使用，不跨域，不纳入 {@code RestTemplateHttpClient}
 * （该工具类定位为 HTTP 通用执行，鉴权头构造保留在业务域内）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MokaAuthSupport {

    private MokaAuthSupport() {
    }

    /**
     * 构造 Moka Basic Auth 请求头
     *
     * @param mokaProperties     Moka 配置（含 apiKey）
     * @param includeContentType true 时同时设置 Content-Type: application/json
     * @return HttpHeaders 实例，含 Authorization 头 + Accept: application/json
     */
    public static HttpHeaders buildBasicAuthHeaders(MokaProperties mokaProperties, boolean includeContentType) {
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
