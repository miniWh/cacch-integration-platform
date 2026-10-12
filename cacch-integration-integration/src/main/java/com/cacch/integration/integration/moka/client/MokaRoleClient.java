package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaRoleListResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Moka 自定义角色 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaRoleClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_LIST = "获取全量自定义角色";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 获取 Moka 全量角色 —— GET /api-platform/v1/users/roles?type=all
     */
    public MokaRoleListResponse listRoles() {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_LIST);
            throw new IllegalStateException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.ROLE_LIST_PATH + "?type=all";
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, false);

        MokaRoleListResponse body = httpClient.doGet(url, headers, BIZ, ACTION_LIST, MokaRoleListResponse.class);

        if (body.isSuccess()) {
            int roleCount = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, roleCount={}", BIZ, ACTION_LIST, roleCount);
        } else {
            log.info("【{}】{}终止, code={}, msg={}",
                    BIZ, ACTION_LIST, body.getCode(), body.getMsg());
        }
        return body;
    }
}
