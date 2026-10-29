package com.cacch.integration.integration.crm.client;

import com.cacch.integration.common.config.crm.CrmProperties;
import com.cacch.integration.common.constant.crm.CrmConstants;
import com.cacch.integration.integration.crm.client.dto.CrmContractEditRequest;
import com.cacch.integration.integration.crm.client.dto.CrmEmployeeQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.client.dto.CrmPageQueryRequest;
import com.cacch.integration.integration.crm.support.CrmDigestSupport;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 勤策 CRM OpenAPI HTTP 客户端
 *
 * <p>HTTP 通用调用（URI 构造、出入参日志、异常兜底）统一由
 * {@link RestTemplateHttpClient} 承担；签名逻辑由 {@link CrmDigestSupport} 处理。</p>
 *
 * <p>注意：签名与请求体使用同一份 JSON 字符串，避免序列化差异导致 digest 校验失败；
 * 响应体先以 String 形式获取，再手动 parseResponse（response_data 可能是 JSON 字符串需二次解析）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmClient {

    private static final String BIZ = CrmConstants.LOG_BIZ;
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final RestTemplateHttpClient httpClient;
    private final CrmProperties crmProperties;

    public CrmOpenApiResponse orderQuery(CrmPageQueryRequest request) {
        return post(crmProperties.getBaseUrl(), CrmConstants.ORDER_QUERY_PATH, request, "查询订单");
    }

    public CrmOpenApiResponse orderDetailQuery(CrmPageQueryRequest request) {
        return post(crmProperties.getBaseUrl(), CrmConstants.ORDER_DETAIL_QUERY_PATH, request, "查询订单明细");
    }

    public CrmOpenApiResponse queryEmployee(CrmEmployeeQueryRequest request) {
        return post(crmProperties.getEmployeeBaseUrl(), CrmConstants.EMPLOYEE_QUERY_PATH, request, "查询员工帐号");
    }

    /**
     * 修改销售合同（REQ-OA-003 合同附件回传）
     *
     * @param request 合同修改请求体：data_id = CRM 合同数据 ID，
     *                fields 含 {@code id} 与附件字段（[{path, name}]，不传 fileSize）
     * @return CRM OpenAPI 统一响应信封；return_code=0 表示成功
     * @throws RestClientException HTTP 调用失败或请求组装异常时抛出
     */
    public CrmOpenApiResponse contractEdit(CrmContractEditRequest request) {
        return post(crmProperties.getBaseUrl(), CrmConstants.CONTRACT_EDIT_PATH, request, "修改销售合同");
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

            ThirdPartyHttpLogSupport.logRequest(BIZ, action, url, bodyJson);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 先以 String 形式拿原始响应体，再手动 parseResponse（response_data 可能是 JSON 字符串）
            String responseBody = httpClient.doPostOrPut(
                    HttpMethod.POST, url, bodyJson, headers, BIZ, action, String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, action);
                throw new RestClientException("勤策 " + action + " 响应体为空");
            }

            CrmOpenApiResponse response = parseResponse(responseBody);

            if (response.isSuccess()) {
                log.info("【{}】{}成功, msgId={}", BIZ, action, response.getMsgId());
            } else {
                log.info("【{}】{}终止, returnCode={}, returnMsg={}, msgId={}",
                        BIZ, action, response.getReturnCode(), response.getReturnMsg(), response.getMsgId());
            }
            return response;
        } catch (RestClientException e) {
            throw e;
        } catch (Exception e) {
            log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
            log.error("【{}】{} 处理失败", BIZ, action, e);
            throw new RestClientException("勤策 " + action + " 处理失败: " + e.getMessage(), e);
        }
    }

    private CrmOpenApiResponse parseResponse(String responseBody) {
        CrmOpenApiResponse response = OBJECT_MAPPER.readValue(responseBody, CrmOpenApiResponse.class);
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
