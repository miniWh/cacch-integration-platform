package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Moka 候选人 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担，本类仅保留 URL 拼接、
 * Basic Auth 构造、成功 INFO。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaCandidatesClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_LIST_APPLICATIONS = "根据条件查询申请信息";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 根据条件查询申请信息（分页）
     */
    public MokaApplicationListResponse listApplications(MokaApplicationListRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_LIST_APPLICATIONS);
            throw new IllegalStateException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.APPLICATION_LIST_PATH;
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

        MokaApplicationListResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ, ACTION_LIST_APPLICATIONS, MokaApplicationListResponse.class);

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
    }
}
