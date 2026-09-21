package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaRoleListResponse;
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
 * Moka 自定义角色 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #listRoles()} — 获取 Moka 全量角色
 *     （GET {@value com.cacch.integration.common.constant.moka.MokaConstants#ROLE_LIST_PATH}?type=all，
 *     对接文档 https://www.mokahr.com/docs/api/?shell#-75）</li>
 * </ul>
 *
 * <p>接口说明：
 * <ul>
 *     <li>查询参数 {@code type} 可选值：{@code all}（全部，含内建 + 自定义）、
 *     {@code custom}（仅自定义）、{@code builtin}（仅内建）。本 Client 固定传
 *     {@code type=all}，确保拉取完整角色列表。</li>
 *     <li>响应字段：{@code id}（角色 ID，对应 DB role_id）、
 *     {@code name}（角色名称，对应 DB role_name）、
 *     {@code role}（角色值，整数）、{@code description}（描述）。</li>
 * </ul>
 *
 * <p>鉴权：HTTP Basic Auth，将机构 API Key 作为 username（password 为空），
 * 即 {@code Authorization: Basic Base64(apiKey + ":")}。API Key 经配置属性
 * {@link MokaProperties#getApiKey()} 注入，严禁记入日志。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaRoleClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_LIST = "获取全量自定义角色";

    private final RestTemplate restTemplate;
    private final MokaProperties mokaProperties;

    /**
     * 获取 Moka 全量角色 —— GET {@code /api-platform/v1/users/roles?type=all}
     *
     * <p>查询参数固定传 {@code type=all}（全部角色，含内建 + 自定义）。
     * Moka 侧角色数量通常较少（一般 ≤ 50），全量拉取无性能问题。</p>
     *
     * @return Moka 角色列表响应（data 为角色项列表，字段：id / name / role / description）
     * @throws RestClientException API Key 未配置、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public MokaRoleListResponse listRoles() {
        // —— 前置守卫：API Key 必须已配置 ——
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置，请在 yml 或环境变量 MOKA_API_KEY 中设置",
                    BIZ, ACTION_LIST);
            throw new RestClientException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.ROLE_LIST_PATH + "?type=all";
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            log.info("【{}】{}终止, reason=URL 非法, url={}", BIZ, ACTION_LIST, url);
            throw new RestClientException("Moka 角色查询 URL 非法: " + url, e);
        }

        // —— 构造 Basic Auth Header（apiKey 作为 username，password 为空）——
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((mokaProperties.getApiKey() + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth);

        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION_LIST, uri.toString(), null);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<MokaRoleListResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, MokaRoleListResponse.class);
            MokaRoleListResponse body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION_LIST, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, ACTION_LIST);
                throw new RestClientException("Moka 角色查询响应为空");
            }

            if (body.isSuccess()) {
                int roleCount = body.getData() == null ? 0 : body.getData().size();
                log.info("【{}】{}成功, roleCount={}", BIZ, ACTION_LIST, roleCount);
            } else {
                log.info("【{}】{}终止, code={}, msg={}",
                        BIZ, ACTION_LIST, body.getCode(), body.getMsg());
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, ACTION_LIST, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, ACTION_LIST, e);
            throw e;
        }
    }
}
