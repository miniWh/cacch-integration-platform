package com.cacch.integration.integration.ihr.client;

import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrConstants;
import com.cacch.integration.integration.ihr.client.dto.IhrPositionListResponse;
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
import java.util.Collections;

/**
 * IHR 职位 HTTP 客户端 — 「获取公司职位清单」
 *
 * <p>接口：GET /openapi/thirdparty/api/v1/org/{orgId}/positions
 * 与 {@link IhrOrgClient}（部门清单 v3）并列，同样使用 Bearer token + RestTemplate。
 * 成功判定：{@code code == 200}（部门接口为 {@code code == 0}）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrPositionClient {

    private static final String BIZ = IhrConstants.LOG_BIZ;
    private static final String ACTION_LIST_POSITIONS = "获取公司职位清单";

    private final RestTemplate restTemplate;
    private final IhrProperties ihrProperties;

    /**
     * 拉取全量职位清单（iHR 无分页，一次返回全部）
     *
     * @param accessToken IHR access_token（不可为空）
     * @return 职位列表响应；业务 code != 200 或 HTTP 非 2xx 抛 RestClientException
     */
    public IhrPositionListResponse listAll(String accessToken) {
        String url = ihrProperties.getBaseUrl()
                + IhrConstants.POSITION_LIST_PATH_TEMPLATE.replace("{orgId}", IhrConstants.ORG_ID);
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RestClientException("IHR 职位清单 URL 非法: " + url, e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION_LIST_POSITIONS, uri.toString(), null);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<IhrPositionListResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, IhrPositionListResponse.class);
            IhrPositionListResponse body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION_LIST_POSITIONS, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, ACTION_LIST_POSITIONS);
                throw new RestClientException("IHR 职位清单响应为空");
            }

            if (body.isSuccess()) {
                int count = body.getData() == null ? 0 : body.getData().size();
                log.info("【{}】{}成功, code={}, currentSize={}", BIZ, ACTION_LIST_POSITIONS, body.getCode(), count);
            } else {
                log.info("【{}】{}终止, code={}, message={}", BIZ, ACTION_LIST_POSITIONS, body.getCode(), body.getMessage());
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, ACTION_LIST_POSITIONS, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, ACTION_LIST_POSITIONS, e);
            throw e;
        }
    }
}
