package com.cacch.integration.integration.crm.client;

import com.cacch.integration.common.config.crm.CrmProperties;
import com.cacch.integration.common.constant.crm.CrmConstants;
import com.cacch.integration.integration.crm.client.dto.CrmEmployeeQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.client.dto.CrmPageQueryRequest;
import com.cacch.integration.integration.crm.support.CrmDigestSupport;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 勤策 CRM OpenAPI HTTP 客户端
 *
 * <p>签名与请求体使用同一份 JSON 字符串，避免序列化差异导致 digest 校验失败。</p>
 * <p>使用本地 {@link JsonMapper}，不依赖 Spring 注入的 ObjectMapper（Boot4 默认无 com.fasterxml ObjectMapper Bean）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmClient {

    private static final String BIZ = CrmConstants.LOG_BIZ;

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final RestTemplate restTemplate;
    private final CrmProperties crmProperties;

    /**
     * 查询订单列表（orderQuery）
     *
     * @param request 分页与过滤条件，不可为空
     * @return 勤策统一响应（含 response_data）
     * @throws RestClientException 网络或 HTTP 错误
     */
    public CrmOpenApiResponse orderQuery(CrmPageQueryRequest request) {
        return post(crmProperties.getBaseUrl(), CrmConstants.ORDER_QUERY_PATH, request, "查询订单");
    }

    /**
     * 查询订单明细（orderDetailQuery）
     *
     * @param request 分页与过滤条件，不可为空
     * @return 勤策统一响应
     * @throws RestClientException 网络或 HTTP 错误
     */
    public CrmOpenApiResponse orderDetailQuery(CrmPageQueryRequest request) {
        return post(crmProperties.getBaseUrl(), CrmConstants.ORDER_DETAIL_QUERY_PATH, request, "查询订单明细");
    }

    /**
     * 查询员工帐号（queryEmployee）
     *
     * @param request 员工查询条件，不可为空
     * @return 勤策统一响应
     * @throws RestClientException 网络或 HTTP 错误
     */
    public CrmOpenApiResponse queryEmployee(CrmEmployeeQueryRequest request) {
        return post(crmProperties.getEmployeeBaseUrl(), CrmConstants.EMPLOYEE_QUERY_PATH, request, "查询员工帐号");
    }

    private CrmOpenApiResponse post(String baseUrl, String pathTemplate, Object body, String action) {
        try {
            String bodyJson = OBJECT_MAPPER.writeValueAsString(body);
            String timestamp = CrmDigestSupport.currentTimestamp();
            String msgId = CrmDigestSupport.newMsgId();
            String digest = CrmDigestSupport.digest(bodyJson, crmProperties.getAppKey(), timestamp);

            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + pathTemplate)
                    .buildAndExpand(crmProperties.getOpenId(), timestamp, digest, msgId)
                    .toUriString();

            // 入参打印实际发出的 JSON（与签名一致），出参打印原始响应体
            ThirdPartyHttpLogSupport.logRequest(BIZ, action, url, bodyJson);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            String responseBody = responseEntity.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                ThirdPartyHttpLogSupport.logResponse(BIZ, action, null);
                log.info("【{}】{}终止, reason=响应体为空", BIZ, action);
                throw new RestClientException("勤策 " + action + " 响应体为空");
            }

            ThirdPartyHttpLogSupport.logResponse(BIZ, action, responseBody);
            CrmOpenApiResponse response = parseResponse(responseBody);

            if (response.isSuccess()) {
                log.info("【{}】{}成功, msgId={}", BIZ, action, response.getMsgId());
            } else {
                log.info("【{}】{}终止, returnCode={}, returnMsg={}, msgId={}",
                        BIZ, action, response.getReturnCode(), response.getReturnMsg(), response.getMsgId());
            }
            return response;
        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, action, e);
            throw e;
        } catch (Exception e) {
            log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
            log.error("【{}】{} 处理失败", BIZ, action, e);
            throw new RestClientException("勤策 " + action + " 处理失败: " + e.getMessage(), e);
        }
    }

    private CrmOpenApiResponse parseResponse(String responseBody) {
        CrmOpenApiResponse response = OBJECT_MAPPER.readValue(responseBody, CrmOpenApiResponse.class);
        // response_data 官方说明可能是 JSON 字符串，二次解析为节点便于调用方使用
        JsonNode dataNode = response.getResponseData();
        if (dataNode != null && dataNode.isString()) {
            String text = dataNode.asString();
            if (text != null && !text.isBlank() && !"null".equalsIgnoreCase(text)) {
                response.setResponseData(OBJECT_MAPPER.readTree(text));
            }
        }
        return response;
    }
}
