package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaUserSyncResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Moka 用户信息 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaUserClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_SYNC = "Moka用户信息同步";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 用户信息同步 —— 以手机号为唯一键执行 upsert
     *
     * @param request 同步请求体（usersInfo 必填且非空）
     * @return Moka 同步结果响应
     * @throws RestClientException 请求体非法时抛出（API Key / HTTP 异常由 RestTemplateHttpClient 抛出）
     */
    public MokaUserSyncResponse syncUserInfo(MokaUserSyncRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_SYNC);
            throw new RestClientException("Moka API Key 未配置");
        }
        if (request == null || request.getUsersInfo() == null || request.getUsersInfo().isEmpty()) {
            log.info("【{}】{}终止, reason=请求体或 usersInfo 为空", BIZ, ACTION_SYNC);
            throw new RestClientException("Moka 用户信息同步请求体为空");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.USER_SYNC_INFO_PATH;
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

        MokaUserSyncResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ, ACTION_SYNC, MokaUserSyncResponse.class);

        if (body.isSuccess()) {
            int userCount = request.getUsersInfo().size();
            log.info("【{}】{}成功, userCount={}", BIZ, ACTION_SYNC, userCount);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_SYNC, body.getCode(), body.getMsg());
        }
        return body;
    }
}
