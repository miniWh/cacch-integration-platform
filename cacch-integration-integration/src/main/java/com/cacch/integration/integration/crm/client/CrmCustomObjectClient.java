package com.cacch.integration.integration.crm.client;

import com.cacch.integration.common.config.crm.CrmProperties;
import com.cacch.integration.common.constant.crm.CrmCustomObjectLogConstants;
import com.cacch.integration.common.constant.crm.CrmCustomObjectPathConstants;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryResponse;
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
}
