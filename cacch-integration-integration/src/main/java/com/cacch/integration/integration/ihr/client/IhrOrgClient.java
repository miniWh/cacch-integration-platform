package com.cacch.integration.integration.ihr.client;

import com.cacch.integration.common.config.ihr.IhrProperties;
import com.cacch.integration.common.constant.ihr.IhrConstants;
import com.cacch.integration.common.constant.ihr.IhrPathConstants;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentDetailResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentListResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;

/**
 * IHR 组织架构/部门 HTTP 客户端
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IhrOrgClient {

    private static final String BIZ = IhrConstants.LOG_BIZ;
    private static final String ACTION_SEARCH_DEPARTMENTS = "获取部门清单v3";
    private static final String ACTION_GET_DEPARTMENT_DETAIL = "获取部门详情";
    private static final String ACTION_LIST_DEPARTMENTS_BY_IDS = "根据部门id集合获取部门详情";

    /**
     * 静态 ObjectMapper —— 用于将原始响应报文反序列化为详情响应
     *
     * <p>与 {@link RestTemplateHttpClient} 使用相同模式（静态 JsonMapper），
     * 不依赖 Spring Bean 注入，避免 Bean 缺失导致启动失败。</p>
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

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

        String fullUrl = ihrProperties.getBaseUrl() + IhrPathConstants.ORG_SEARCH_DEPARTMENT_PATH;
        log.info("【{}】{}, 完整url={}", BIZ, ACTION_SEARCH_DEPARTMENTS, fullUrl);
        HttpHeaders headers = buildBearerHeaders(accessToken, true);

        IhrOrgSearchResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, fullUrl, request, headers, BIZ, ACTION_SEARCH_DEPARTMENTS, IhrOrgSearchResponse.class);

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

    /**
     * 获取部门详情（单部门查询，按部门编码）
     *
     * @param accessToken    IHR access_token（来自 Token 服务缓存），不可为空
     * @param departmentCode 部门编码
     * @return 部门详情响应（含 rawJson 原文）；业务成功以 {@link IhrDepartmentDetailResponse#isSuccess()} 判定
     * @throws RestClientException HTTP 调用异常或响应体反序列化失败时抛出
     */
    public IhrDepartmentDetailResponse getDepartmentDetail(String accessToken, String departmentCode) {
        if (StringUtils.isBlank(departmentCode)) {
            throw new RestClientException("IHR 部门详情查询 departmentCode 为空");
        }

        String fullUrl = ihrProperties.getBaseUrl() + String.format(IhrPathConstants.ORG_DEPARTMENT_DETAIL_PATH, departmentCode);
        log.info("【{}】{}, 完整url={}", BIZ, ACTION_GET_DEPARTMENT_DETAIL, fullUrl);
        HttpHeaders headers = buildBearerHeaders(accessToken, false);

        String rawBody = httpClient.doGet(fullUrl, headers, BIZ, ACTION_GET_DEPARTMENT_DETAIL, String.class);

        IhrDepartmentDetailResponse body;
        try {
            body = MAPPER.readValue(rawBody, IhrDepartmentDetailResponse.class);
        } catch (Exception e) {
            log.info("【{}】{}终止, reason=响应体反序列化失败, departmentCode={}", BIZ, ACTION_GET_DEPARTMENT_DETAIL, departmentCode);
            log.error("【{}】{} 响应体反序列化失败, departmentCode={}", BIZ, ACTION_GET_DEPARTMENT_DETAIL, departmentCode, e);
            throw new RestClientException(BIZ + " " + ACTION_GET_DEPARTMENT_DETAIL + " 响应体反序列化失败: " + e.getMessage(), e);
        }
        body.setRawJson(rawBody);

        if (body.isSuccess()) {
            boolean hasData = body.getData() != null;
            log.info("【{}】{}成功, code=0, departmentCode={}, hasData={}", BIZ, ACTION_GET_DEPARTMENT_DETAIL, departmentCode, hasData);
        } else {
            log.info("【{}】{}终止, departmentCode={}, code={}, message={}", BIZ, ACTION_GET_DEPARTMENT_DETAIL,
                    departmentCode, body.getCode(), body.getMessage());
        }
        return body;
    }

    /**
     * 根据部门 id 集合批量获取部门详情
     *
     * @param accessToken   IHR access_token（来自 Token 服务缓存），不可为空
     * @param departmentIds 部门 ID 集合（非空；单批建议 ≤100）
     * @return 部门详情列表响应（含 rawJson 原文）；业务成功以 {@link IhrDepartmentListResponse#isSuccess()} 判定
     * @throws RestClientException HTTP 调用异常或响应体反序列化失败时抛出
     */
    public IhrDepartmentListResponse listDepartmentsByIds(String accessToken, List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            throw new RestClientException("IHR 根据部门id集合获取部门详情 departmentIds 为空");
        }

        String fullUrl = ihrProperties.getBaseUrl() + IhrPathConstants.ORG_DEPARTMENT_LIST_BY_IDS_PATH;
        log.info("【{}】{}, 完整url={}, deptIdCount={}", BIZ, ACTION_LIST_DEPARTMENTS_BY_IDS, fullUrl, departmentIds.size());
        HttpHeaders headers = buildBearerHeaders(accessToken, true);

        String rawBody = httpClient.doPostOrPut(
                HttpMethod.POST, fullUrl, departmentIds, headers, BIZ, ACTION_LIST_DEPARTMENTS_BY_IDS, String.class);

        IhrDepartmentListResponse body;
        try {
            body = MAPPER.readValue(rawBody, IhrDepartmentListResponse.class);
        } catch (Exception e) {
            log.info("【{}】{}终止, reason=响应体反序列化失败, deptIdCount={}", BIZ, ACTION_LIST_DEPARTMENTS_BY_IDS, departmentIds.size());
            log.error("【{}】{} 响应体反序列化失败, deptIdCount={}", BIZ, ACTION_LIST_DEPARTMENTS_BY_IDS, departmentIds.size(), e);
            throw new RestClientException(BIZ + " " + ACTION_LIST_DEPARTMENTS_BY_IDS + " 响应体反序列化失败: " + e.getMessage(), e);
        }
        body.setRawJson(rawBody);

        if (body.isSuccess()) {
            int count = body.getData() == null ? 0 : body.getData().size();
            log.info("【{}】{}成功, code=0, deptIdCount={}, returnCount={}",
                    BIZ, ACTION_LIST_DEPARTMENTS_BY_IDS, departmentIds.size(), count);
        } else {
            log.info("【{}】{}终止, deptIdCount={}, code={}, message={}", BIZ, ACTION_LIST_DEPARTMENTS_BY_IDS,
                    departmentIds.size(), body.getCode(), body.getMessage());
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
