package com.cacch.integration.integration.ihr.client;

import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrConstants;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Collections;

/**
 * IHR 组织架构/部门 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担；
 * Bearer Token 鉴权头由本类构造。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrOrgClient {

    private static final String BIZ = IhrConstants.LOG_BIZ;
    private static final String ACTION_SEARCH_DEPARTMENTS = "获取部门清单v3";

    private final RestTemplateHttpClient httpClient;
    private final IhrProperties ihrProperties;

    /**
     * 获取部门清单（分页 + 条件查询）
     *
     * @param accessToken IHR access_token（来自 Token 服务缓存），不可为空
     * @param request     分页与搜索条件，不可为空
     * @return 部门分页结果
     * @throws RestClientException 网络或 HTTP 非 2xx 抛出
     */
    public IhrOrgSearchResponse searchDepartments(String accessToken, IhrOrgSearchRequest request) {
        if (request == null) {
            throw new RestClientException("IHR 部门查询请求体为空");
        }

        String url = ihrProperties.getBaseUrl() + IhrConstants.ORG_SEARCH_DEPARTMENT_PATH;
        HttpHeaders headers = buildBearerHeaders(accessToken, true);

        IhrOrgSearchResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ, ACTION_SEARCH_DEPARTMENTS, IhrOrgSearchResponse.class);

        if (body.isSuccess()) {
            int count = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, totalElements={}, currentSize={}, end={}",
                    BIZ, ACTION_SEARCH_DEPARTMENTS, body.getTotalElements(), count, body.getEnd());
        } else {
            log.info("【{}】{}终止, code={}, message={}", BIZ, ACTION_SEARCH_DEPARTMENTS,
                    body.getCode(), body.getMessage());
        }
        return body;
    }

    private HttpHeaders buildBearerHeaders(String accessToken, boolean includeContentType) {
        HttpHeaders headers = new HttpHeaders();
        if (includeContentType) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return headers;
    }
}
