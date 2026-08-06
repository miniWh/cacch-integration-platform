package com.cacch.integration.integration.fdd.client;

import com.cacch.integration.common.config.fdd.FddProperties;
import com.cacch.integration.common.constant.fdd.FddConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.fdd.client.dto.FddCreateAccountRequest;
import com.cacch.integration.integration.fdd.client.dto.FddCreateAccountResponse;
import com.cacch.integration.integration.fdd.client.dto.FddCreateCompanyRequest;
import com.cacch.integration.integration.fdd.client.dto.FddCreateCompanyResponse;
import com.cacch.integration.integration.fdd.client.dto.FddEnterpriseAuthRequest;
import com.cacch.integration.integration.fdd.client.dto.FddEnterpriseAuthResponse;
import com.cacch.integration.integration.fdd.client.dto.FddGetAccountResponse;
import com.cacch.integration.integration.fdd.client.dto.FddGetCompanyResponse;
import com.cacch.integration.integration.fdd.client.dto.FddPersonAuthRequest;
import com.cacch.integration.integration.fdd.client.dto.FddPersonAuthResponse;
import com.cacch.integration.integration.fdd.support.FddTokenSupport;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 法大大 HTTP 客户端（用户/企业创建、实名认证 URL 获取）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FddClient {

    private static final String BIZ = FddConstants.LOG_BIZ;
    private static final String ACTION_ENTERPRISE = "企业认证URL";
    private static final String ACTION_PERSON = "个人认证URL";
    private static final String ACTION_CREATE_ACCOUNT = "创建用户";
    private static final String ACTION_GET_ACCOUNT = "查询用户";
    private static final String ACTION_CREATE_COMPANY = "创建企业";
    private static final String ACTION_GET_COMPANY = "查询企业";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final RestTemplate restTemplate;
    private final FddProperties fddProperties;
    private final FddTokenSupport fddTokenSupport;

    /**
     * 创建电子签用户账号
     *
     * @param request 创建用户请求
     * @return 创建结果（含 accountId）
     * @throws BizException 调用失败时抛出
     */
    public FddCreateAccountResponse createAccount(FddCreateAccountRequest request) {
        return postJson(ACTION_CREATE_ACCOUNT, joinBase(FddConstants.PATH_CREATE_ACCOUNT),
                request, FddCreateAccountResponse.class, "tpAccountId=" + maskId(request.getTpAccountId()));
    }

    /**
     * 按第三方账号或手机号查询用户
     *
     * @param tpAccountId 第三方用户标识（可空）
     * @param mobile      手机号（可空）
     * @return 查询响应；无用户时 data 为空
     * @throws BizException HTTP 失败时抛出
     */
    public FddGetAccountResponse getAccount(String tpAccountId, String mobile) {
        Map<String, String> query = new LinkedHashMap<>();
        if (StringUtils.hasText(tpAccountId)) {
            query.put("tpAccountId", tpAccountId.trim());
        }
        if (StringUtils.hasText(mobile)) {
            query.put("mobile", mobile.trim());
        }
        if (query.isEmpty()) {
            throw new BizException(ResultCode.PARAM_MISSING, "查询用户须提供 tpAccountId 或 mobile");
        }
        return getForm(ACTION_GET_ACCOUNT, joinBase(FddConstants.PATH_GET_ACCOUNT), query,
                FddGetAccountResponse.class, "tpAccountId=" + maskId(tpAccountId));
    }

    /**
     * 创建外部企业并绑定系统管理员
     *
     * @param request 创建企业请求
     * @return 创建结果（含 companyId、accountId）
     * @throws BizException 调用失败时抛出
     */
    public FddCreateCompanyResponse createCompany(FddCreateCompanyRequest request) {
        return postJson(ACTION_CREATE_COMPANY, joinBase(FddConstants.PATH_CREATE_COMPANY),
                request, FddCreateCompanyResponse.class, "tpOrgId=" + request.getTpOrgId());
    }

    /**
     * 查询企业详情
     *
     * <p>法大大多条件为 AND，调用方宜单条件查询（companyId / companyName / creditNo / tpOrgId 择一）。</p>
     *
     * @param companyId   法大大企业 ID（可空）
     * @param companyName 企业名称（可空）
     * @param creditNo    统一社会信用代码（可空）
     * @param tpOrgId     第三方企业标识（可空）
     * @return 查询响应；无企业时 data 为空
     * @throws BizException 参数全空或 HTTP 失败时抛出
     */
    public FddGetCompanyResponse getCompany(String companyId, String companyName,
                                            String creditNo, String tpOrgId) {
        Map<String, String> query = new LinkedHashMap<>();
        if (StringUtils.hasText(companyId)) {
            query.put("companyId", companyId.trim());
        }
        if (StringUtils.hasText(companyName)) {
            query.put("companyName", companyName.trim());
        }
        if (StringUtils.hasText(creditNo)) {
            query.put("creditNo", creditNo.trim());
        }
        if (StringUtils.hasText(tpOrgId)) {
            query.put("tpOrgId", tpOrgId.trim());
        }
        if (query.isEmpty()) {
            throw new BizException(ResultCode.PARAM_MISSING,
                    "查询企业须提供 companyId、companyName、creditNo 或 tpOrgId 之一");
        }
        return getForm(ACTION_GET_COMPANY, joinBase(FddConstants.PATH_GET_COMPANY), query,
                FddGetCompanyResponse.class, "queryKeys=" + query.keySet());
    }

    /**
     * 获取企业实名认证页面 URL
     *
     * @param request 企业认证请求体
     * @return 法大大响应（含 url、transactionNo）
     * @throws BizException 配置缺失、HTTP 失败或业务码非成功时抛出
     */
    public FddEnterpriseAuthResponse getEnterpriseAuthUrl(FddEnterpriseAuthRequest request) {
        if (request == null) {
            log.info("【{}】{}终止, reason=请求体为空", BIZ, ACTION_ENTERPRISE);
            throw new BizException(ResultCode.PARAM_MISSING, "企业认证请求不能为空");
        }
        if (!StringUtils.hasText(fddProperties.getEnterpriseAuthUrl())) {
            log.info("【{}】{}终止, reason=enterpriseAuthUrl 未配置", BIZ, ACTION_ENTERPRISE);
            throw new BizException(ResultCode.PARAM_INVALID, "法大大企业认证地址未配置");
        }

        FddEnterpriseAuthResponse response = postJson(ACTION_ENTERPRISE, fddProperties.getEnterpriseAuthUrl(),
                request, FddEnterpriseAuthResponse.class, "uscc=" + request.getTpOrgId());
        if (response == null) {
            log.info("【{}】{}终止, reason=响应为空, uscc={}", BIZ, ACTION_ENTERPRISE, request.getTpOrgId());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大企业认证接口返回空响应");
        }
        if (!response.isSuccess()) {
            log.info("【{}】{}终止, code={}, message={}, uscc={}",
                    BIZ, ACTION_ENTERPRISE, response.getCode(), response.getMessage(), request.getTpOrgId());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "法大大企业认证失败: code=" + response.getCode() + ", message=" + response.getMessage());
        }
        log.info("【{}】{}成功, uscc={}, transactionNo={}",
                BIZ, ACTION_ENTERPRISE, request.getTpOrgId(),
                response.getData() != null ? response.getData().getTransactionNo() : null);
        return response;
    }

    /**
     * 获取个人实名认证页面 URL
     *
     * @param request 个人认证请求体
     * @return 法大大响应（含 url、transactionNo）
     * @throws BizException 配置缺失、HTTP 失败或业务码非成功时抛出
     */
    public FddPersonAuthResponse getPersonAuthUrl(FddPersonAuthRequest request) {
        if (request == null) {
            log.info("【{}】{}终止, reason=请求体为空", BIZ, ACTION_PERSON);
            throw new BizException(ResultCode.PARAM_MISSING, "个人认证请求不能为空");
        }
        if (!StringUtils.hasText(fddProperties.getPersonAuthUrl())) {
            log.info("【{}】{}终止, reason=personAuthUrl 未配置", BIZ, ACTION_PERSON);
            throw new BizException(ResultCode.PARAM_INVALID, "法大大个人认证地址未配置");
        }

        FddPersonAuthResponse response = postJson(ACTION_PERSON, fddProperties.getPersonAuthUrl(),
                request, FddPersonAuthResponse.class, "tpAccountId=" + maskId(request.getTpAccountId()));
        if (response == null) {
            log.info("【{}】{}终止, reason=响应为空, tpAccountId={}",
                    BIZ, ACTION_PERSON, maskId(request.getTpAccountId()));
            throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大个人认证接口返回空响应");
        }
        if (!response.isSuccess()) {
            log.info("【{}】{}终止, code={}, message={}, tpAccountId={}",
                    BIZ, ACTION_PERSON, response.getCode(), response.getMessage(),
                    maskId(request.getTpAccountId()));
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "法大大个人认证失败: code=" + response.getCode() + ", message=" + response.getMessage());
        }
        log.info("【{}】{}成功, tpAccountId={}, transactionNo={}",
                BIZ, ACTION_PERSON, maskId(request.getTpAccountId()),
                response.getData() != null ? response.getData().getTransactionNo() : null);
        return response;
    }

    private <T> T postJson(String action, String url, Object body, Class<T> responseType, String bizKey) {
        String accessToken = fddTokenSupport.getAccessToken();
        try {
            String bodyJson = OBJECT_MAPPER.writeValueAsString(body);
            ThirdPartyHttpLogSupport.logRequest(BIZ, action, url, bodyJson);

            HttpHeaders headers = authHeaders(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            T response = restTemplate.postForObject(url, entity, responseType);
            ThirdPartyHttpLogSupport.logResponse(BIZ, action, response);
            return response;
        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】{}终止, {}, reason={}", BIZ, action, bizKey, e.getMessage());
            log.error("【{}】{} HTTP 调用失败, {}", BIZ, action, bizKey, e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "法大大" + action + "超时或网络异常", e);
        } catch (Exception e) {
            log.info("【{}】{}终止, {}, reason={}", BIZ, action, bizKey, e.getMessage());
            log.error("【{}】{} 处理失败, {}", BIZ, action, bizKey, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大" + action + "调用失败", e);
        }
    }

    private <T> T getForm(String action, String url, Map<String, String> query, Class<T> responseType, String bizKey) {
        String accessToken = fddTokenSupport.getAccessToken();
        try {
            // build() 后再 encode，避免中文等非 ASCII 查询参数触发 IllegalArgumentException
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            query.forEach(builder::queryParam);
            URI uri = builder.build().encode().toUri();
            ThirdPartyHttpLogSupport.logRequest(BIZ, action, uri.toString(), query);

            HttpHeaders headers = authHeaders(accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<T> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, entity, responseType);
            T response = responseEntity.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, action, response);
            return response;
        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】{}终止, {}, reason={}", BIZ, action, bizKey, e.getMessage());
            log.error("【{}】{} HTTP 调用失败, {}", BIZ, action, bizKey, e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "法大大" + action + "超时或网络异常", e);
        } catch (Exception e) {
            log.info("【{}】{}终止, {}, reason={}", BIZ, action, bizKey, e.getMessage());
            log.error("【{}】{} 处理失败, {}", BIZ, action, bizKey, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大" + action + "调用失败", e);
        }
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "bearer " + accessToken);
        return headers;
    }

    private String joinBase(String path) {
        if (!StringUtils.hasText(fddProperties.getBaseUrl())) {
            throw new BizException(ResultCode.PARAM_INVALID, "法大大 base-url 未配置");
        }
        return fddProperties.getBaseUrl() + path;
    }

    private static String maskId(String id) {
        if (!StringUtils.hasText(id) || id.length() < 10) {
            return "****";
        }
        return id.substring(0, 6) + "********" + id.substring(id.length() - 4);
    }
}
