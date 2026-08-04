package com.cacch.integration.integration.fdd.client;

import com.cacch.integration.common.config.fdd.FddProperties;
import com.cacch.integration.common.constant.fdd.FddConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.fdd.client.dto.FddEnterpriseAuthRequest;
import com.cacch.integration.integration.fdd.client.dto.FddEnterpriseAuthResponse;
import com.cacch.integration.integration.fdd.support.FddTokenSupport;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 法大大 HTTP 客户端（企业实名认证 URL 获取）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FddClient {

    private static final String BIZ = FddConstants.LOG_BIZ;
    private static final String ACTION_ENTERPRISE = "企业认证URL";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final RestTemplate restTemplate;
    private final FddProperties fddProperties;
    private final FddTokenSupport fddTokenSupport;

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

        String accessToken = fddTokenSupport.getAccessToken();
        String url = appendAccessToken(fddProperties.getEnterpriseAuthUrl(), accessToken);

        try {
            String bodyJson = OBJECT_MAPPER.writeValueAsString(request);
            ThirdPartyHttpLogSupport.logRequest(BIZ, ACTION_ENTERPRISE, url, bodyJson);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            FddEnterpriseAuthResponse response = restTemplate.postForObject(url, entity, FddEnterpriseAuthResponse.class);
            ThirdPartyHttpLogSupport.logResponse(BIZ, ACTION_ENTERPRISE, response);

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
        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】{}终止, uscc={}, reason={}", BIZ, ACTION_ENTERPRISE, request.getTpOrgId(), e.getMessage());
            log.error("【{}】{} HTTP 调用失败, uscc={}", BIZ, ACTION_ENTERPRISE, request.getTpOrgId(), e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "法大大企业认证接口超时或网络异常", e);
        } catch (Exception e) {
            log.info("【{}】{}终止, uscc={}, reason={}", BIZ, ACTION_ENTERPRISE, request.getTpOrgId(), e.getMessage());
            log.error("【{}】{} 处理失败, uscc={}", BIZ, ACTION_ENTERPRISE, request.getTpOrgId(), e);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "法大大企业认证调用失败", e);
        }
    }

    private static String appendAccessToken(String baseUrl, String accessToken) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "access_token=" + accessToken;
    }
}
