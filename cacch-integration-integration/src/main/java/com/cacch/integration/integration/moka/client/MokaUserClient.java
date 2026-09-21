package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncResponse;
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
 * Moka 用户信息 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #syncUserInfo(MokaUserSyncRequest)} — 用户信息同步
 *     （POST {@value com.cacch.integration.common.constant.moka.MokaConstants#USER_SYNC_INFO_PATH}，
 *     对接文档 https://www.mokahr.com/docs/api/#-72）</li>
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
public class MokaUserClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_SYNC = "Moka用户信息同步";

    private final RestTemplate restTemplate;
    private final MokaProperties mokaProperties;

    /**
     * 用户信息同步 —— 以手机号为唯一键执行 upsert（存在则更新、不存在则创建）
     *
     * <p>Moka API 单次推送上限 100 条，调用方（Manager 层）须自行分批。
     * 响应只反馈整批调用结果，不返回单个用户的处理状态，
     * 因此本 Client 视整批成功为成功、整批失败为失败。</p>
     *
     * @param request 同步请求体（usersInfo 必填且非空）
     * @return Moka 同步结果响应
     * @throws RestClientException API Key 未配置、URL 非法、HTTP 调用异常或响应体为空时抛出
     */
    public MokaUserSyncResponse syncUserInfo(MokaUserSyncRequest request) {
        // —— 前置守卫：API Key 必须已配置 ——
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置，请在 yml 或环境变量 MOKA_API_KEY 中设置",
                    BIZ, ACTION_SYNC);
            throw new RestClientException("Moka API Key 未配置");
        }

        if (request == null || request.getUsersInfo() == null || request.getUsersInfo().isEmpty()) {
            log.info("【{}】{}终止, reason=请求体或 usersInfo 为空", BIZ, ACTION_SYNC);
            throw new RestClientException("Moka 用户信息同步请求体为空");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.USER_SYNC_INFO_PATH;
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            log.info("【{}】{}终止, reason=URL 非法, url={}", BIZ, ACTION_SYNC, url);
            throw new RestClientException("Moka 用户信息同步 URL 非法: " + url, e);
        }

        // —— 构造 Basic Auth Header（apiKey 作为 username，password 为空）——
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((mokaProperties.getApiKey() + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, basicAuth);

        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION_SYNC, uri.toString(), request);

        try {
            HttpEntity<MokaUserSyncRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<MokaUserSyncResponse> response = restTemplate.exchange(
                    uri, HttpMethod.POST, entity, MokaUserSyncResponse.class);
            MokaUserSyncResponse body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION_SYNC, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, ACTION_SYNC);
                throw new RestClientException("Moka 用户信息同步响应为空");
            }

            if (body.isSuccess()) {
                int userCount = request.getUsersInfo().size();
                log.info("【{}】{}成功, userCount={}", BIZ, ACTION_SYNC, userCount);
            } else {
                log.info("【{}】{}终止, code={}, msg={}",
                        BIZ, ACTION_SYNC, body.getCode(), body.getMsg());
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, ACTION_SYNC, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, ACTION_SYNC, e);
            throw e;
        }
    }
}
