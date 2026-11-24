package com.cacch.integration.integration.crm.client;

import com.cacch.integration.common.config.crm.CrmProperties;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.support.CrmDigestSupport;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 勤策 CRM OpenAPI 客户端抽象基类
 *
 * <p>沉淀各业务域 Client 共用的 V1 MD5 签名、请求体序列化、URI 构造、
 * 响应信封解析与异常兜底；具体接口由各业务域子类声明，新增接口优先继承本类，
 * 避免重复实现签名与响应解析逻辑。</p>
 *
 * <p>注意：签名与请求体使用同一份 JSON 字符串，避免序列化差异导致 digest 校验失败；
 * 响应体先以 String 形式获取，再手动 parseResponse（response_data 可能是 JSON 字符串需二次解析）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCrmClient {

    protected static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    protected final RestTemplateHttpClient httpClient;
    protected final CrmProperties crmProperties;

    /**
     * 发起勤策 OpenAPI POST 调用（V1 MD5 签名）
     *
     * <p>入参/出参日志统一由 {@link RestTemplateHttpClient} 打印，业务方法无需重复输出。</p>
     *
     * @param baseUrl      接口域名，如 crm.base-url
     * @param pathTemplate 带占位符的路径模板：/{openid}/{timestamp}/{digest}/{msgId}
     * @param body         请求体对象，未赋值字段不会被序列化
     * @param biz          日志业务标识，用于日志前缀【biz】
     * @param action       接口中文名称，用于日志描述
     * @return 已解析的勤策统一响应信封
     * @throws RestClientException HTTP 调用失败或响应解析异常时抛出
     */
    protected CrmOpenApiResponse post(String baseUrl, String pathTemplate, Object body, String biz, String action) {
        try {
            String bodyJson = OBJECT_MAPPER.writeValueAsString(body);
            String timestamp = CrmDigestSupport.currentTimestamp();
            String msgId = CrmDigestSupport.newMsgId();
            String digest = CrmDigestSupport.digest(bodyJson, crmProperties.getAppKey(), timestamp);

            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + pathTemplate)
                    .buildAndExpand(crmProperties.getOpenId(), timestamp, digest, msgId)
                    .toUriString();

            // 入参/出参日志统一由 RestTemplateHttpClient 打印，此处不再重复输出

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 先以 String 形式拿原始响应体，再手动 parseResponse（response_data 可能是 JSON 字符串）
            String responseBody = httpClient.doPostOrPut(
                    HttpMethod.POST, url, bodyJson, headers, biz, action, String.class);

            if (StringUtils.isBlank(responseBody)) {
                log.info("【{}】{}终止, reason=响应体为空", biz, action);
                throw new RestClientException("勤策 " + action + " 响应体为空");
            }

            CrmOpenApiResponse response = parseResponse(responseBody);

            if (response.isSuccess()) {
                log.info("【{}】{}成功, msgId={}", biz, action, response.getMsgId());
            } else {
                log.info("【{}】{}终止, returnCode={}, returnMsg={}, msgId={}",
                        biz, action, response.getReturnCode(), response.getReturnMsg(), response.getMsgId());
            }
            return response;
        } catch (RestClientException e) {
            throw e;
        } catch (Exception e) {
            log.info("【{}】{}终止, reason={}", biz, action, e.getMessage());
            log.error("【{}】{} 处理失败", biz, action, e);
            throw new RestClientException("勤策 " + action + " 处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析勤策统一响应信封，response_data 为 JSON 字符串时二次解析为 JSON 树
     *
     * @param responseBody 原始响应体
     * @return 已解析的响应信封
     */
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
