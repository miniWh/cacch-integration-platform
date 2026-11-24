package com.cacch.integration.integration.crm.client;

import com.cacch.integration.common.config.crm.CrmProperties;
import com.cacch.integration.common.constant.crm.CrmCustomObjectLogConstants;
import com.cacch.integration.common.constant.crm.CrmCustomObjectPathConstants;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryResponse;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectSavePayload;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectSaveRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectSaveResponse;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * 勤策 CRM 自定义对象（apaas）OpenAPI 客户端
 *
 * <p>承载自定义对象域全部接口，签名与响应解析复用 {@link AbstractCrmClient}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
public class CrmCustomObjectClient extends AbstractCrmClient {

    private static final TypeReference<List<Map<String, Object>>> DATA_TYPE = new TypeReference<>() {
    };

    public CrmCustomObjectClient(RestTemplateHttpClient httpClient, CrmProperties crmProperties) {
        super(httpClient, crmProperties);
    }

    /**
     * 查询自定义对象数据
     *
     * <p>response_data 为动态字段对象数组，字段由对象配置决定，故按弱类型 Map 返回。</p>
     *
     * @param request 查询条件，可为空；为空时按空条件查询
     * @return 自定义对象数据响应，response_data 已解析为对象列表
     * @throws RestClientException HTTP 调用失败或响应解析异常时抛出
     */
    public CrmCustomObjectQueryResponse queryObjectData(CrmCustomObjectQueryRequest request) {
        CrmCustomObjectQueryRequest query = request != null ? request : new CrmCustomObjectQueryRequest();
        CrmOpenApiResponse raw = post(crmProperties.getBaseUrl(),
                CrmCustomObjectPathConstants.QUERY_OBJECT_DATA_PATH,
                query, CrmCustomObjectLogConstants.LOG_BIZ, "查询自定义对象数据");

        CrmCustomObjectQueryResponse response = new CrmCustomObjectQueryResponse();
        response.setReturnCode(raw.getReturnCode());
        response.setReturnMsg(raw.getReturnMsg());
        response.setMsgId(raw.getMsgId());
        JsonNode dataNode = raw.getResponseData();
        if (raw.isSuccess() && dataNode != null && !dataNode.isNull()) {
            List<Map<String, Object>> data = OBJECT_MAPPER.convertValue(dataNode, DATA_TYPE);
            response.setResponseData(data == null ? List.of() : data);
        }
        return response;
    }

    /**
     * 创建自定义对象数据
     *
     * <p>勤策要求 fields、slave_fields 为 JSON 格式字符串，此处将入站 Map 序列化后组装出站报文；
     * 签名与请求体使用同一份序列化结果。</p>
     *
     * @param request 创建请求，object_key 必填
     * @return 创建响应，response_data 为新建数据标识节点（含 id）
     * @throws RestClientException HTTP 调用失败或响应解析异常时抛出
     */
    public CrmCustomObjectSaveResponse saveObjectData(CrmCustomObjectSaveRequest request) {
        CrmCustomObjectSavePayload payload = CrmCustomObjectSavePayload.builder()
                .objectKey(request.getObjectKey())
                .fields(toJsonString(request.getFields()))
                .slaveFields(toJsonString(request.getSlaveFields()))
                .isTrigger(request.getIsTrigger())
                .triggerEmployee(request.getTriggerEmployee())
                .checkRule(request.getCheckRule())
                .build();

        CrmOpenApiResponse raw = post(crmProperties.getBaseUrl(),
                CrmCustomObjectPathConstants.SAVE_OBJECT_DATA_PATH,
                payload, CrmCustomObjectLogConstants.LOG_BIZ, "创建自定义对象数据");

        CrmCustomObjectSaveResponse response = new CrmCustomObjectSaveResponse();
        response.setReturnCode(raw.getReturnCode());
        response.setReturnMsg(raw.getReturnMsg());
        response.setMsgId(raw.getMsgId());
        response.setResponseData(raw.getResponseData());
        return response;
    }

    /**
     * 将 Map 序列化为 JSON 字符串，null 保持 null 以便出站时被忽略
     *
     * @param value 待序列化对象
     * @return JSON 字符串；入参为 null 时返回 null
     */
    private String toJsonString(Object value) {
        return value == null ? null : OBJECT_MAPPER.writeValueAsString(value);
    }
}
