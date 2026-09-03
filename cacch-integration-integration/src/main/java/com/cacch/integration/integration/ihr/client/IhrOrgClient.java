package com.cacch.integration.integration.ihr.client;

import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrConstants;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
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
 * IHR 组织架构/部门 HTTP 客户端
 *
 * <p>当前实现：
 * <ul>
 *     <li>{@link #searchDepartments(String, IhrOrgSearchRequest)} — 获取部门清单 v3</li>
 * </ul>
 *
 * <p>所有业务接口统一在 Header 中携带 {@code Authorization: Bearer {access_token}}。</p>
 *
 * <p>网关地址取自 {@link IhrProperties#getBaseUrl()}（yml {@code ihr.base-url}），
 * 与 {@link IhrConstants#ORG_SEARCH_DEPARTMENT_PATH} 在运行时拼接；代码中不硬编码任何环境地址，
 * 测试 / 生产 / 内网域名切换只改配置，无需重新打包。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrOrgClient {

    private static final String BIZ = IhrConstants.LOG_BIZ;
    private static final String ACTION_SEARCH_DEPARTMENTS = "获取部门清单v3";

    private final RestTemplate restTemplate;
    private final IhrProperties ihrProperties;

    /**
     * 获取部门清单（分页 + 条件查询）
     *
     * @param accessToken IHR access_token（来自 Token 服务缓存），不可为空
     * @param request     分页与搜索条件，不可为空
     * @return 部门分页结果
     * @throws RestClientException 网络或 HTTP 非 2xx 抛出；响应 code 非 0 也视为业务失败
     */
    public IhrOrgSearchResponse searchDepartments(String accessToken, IhrOrgSearchRequest request) {
        if (request == null) {
            throw new RestClientException("IHR 部门查询请求体为空");
        }

        String url = ihrProperties.getBaseUrl() + IhrConstants.ORG_SEARCH_DEPARTMENT_PATH;
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RestClientException("IHR 部门查询 URL 非法: " + url, e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION_SEARCH_DEPARTMENTS, uri.toString(), request);

        try {
            HttpEntity<IhrOrgSearchRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<IhrOrgSearchResponse> response = restTemplate.exchange(
                    uri, HttpMethod.POST, entity, IhrOrgSearchResponse.class);
            IhrOrgSearchResponse body = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION_SEARCH_DEPARTMENTS, body);

            if (body == null) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, ACTION_SEARCH_DEPARTMENTS);
                throw new RestClientException("IHR 部门查询响应为空");
            }

            if (body.isSuccess()) {
                int count = body.getData() == null ? 0 : body.getData().size();
                log.info("【{}】{}成功, totalElements={}, currentSize={}, end={}",
                        BIZ, ACTION_SEARCH_DEPARTMENTS, body.getTotalElements(), count, body.getEnd());
            } else {
                log.info("【{}】{}终止, code={}, message={}", BIZ, ACTION_SEARCH_DEPARTMENTS,
                        body.getCode(), body.getMessage());
            }
            return body;

        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, ACTION_SEARCH_DEPARTMENTS, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, ACTION_SEARCH_DEPARTMENTS, e);
            throw e;
        }
    }
}
