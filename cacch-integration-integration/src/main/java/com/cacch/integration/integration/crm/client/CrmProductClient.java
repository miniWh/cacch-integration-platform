package com.cacch.integration.integration.crm.client;

import com.cacch.integration.common.config.crm.CrmProperties;
import com.cacch.integration.common.constant.crm.CrmProductPathConstants;
import com.cacch.integration.common.constant.crm.CrmProductLogConstants;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.client.dto.CrmProductInfo;
import com.cacch.integration.integration.crm.client.dto.CrmProductQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmProductQueryResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;

/**
 * 勤策 CRM 商品域 OpenAPI 客户端
 *
 * <p>承载商品域全部接口，签名与响应解析复用 {@link AbstractCrmClient}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
public class CrmProductClient extends AbstractCrmClient {

    public CrmProductClient(RestTemplateHttpClient httpClient, CrmProperties crmProperties) {
        super(httpClient, crmProperties);
    }

    /**
     * 查询商品
     *
     * <p>请求字段全部可选；未赋值的条件不会出现在请求体与签名串中。</p>
     *
     * @param request 商品查询条件，可为空；为空时按空条件查询
     * @return 商品查询响应，response_data 已解析为商品列表
     * @throws RestClientException HTTP 调用失败或响应解析异常时抛出
     */
    public CrmProductQueryResponse queryProducts(CrmProductQueryRequest request) {
        CrmProductQueryRequest query = request != null ? request : new CrmProductQueryRequest();
        CrmOpenApiResponse raw = post(crmProperties.getBaseUrl(), CrmProductPathConstants.QUERY_PRODUCT_PATH,
                query, CrmProductLogConstants.LOG_BIZ, "查询商品");

        CrmProductQueryResponse response = new CrmProductQueryResponse();
        response.setReturnCode(raw.getReturnCode());
        response.setReturnMsg(raw.getReturnMsg());
        response.setMsgId(raw.getMsgId());
        JsonNode dataNode = raw.getResponseData();
        if (raw.isSuccess() && dataNode != null && !dataNode.isNull()) {
            CrmProductInfo[] products = OBJECT_MAPPER.convertValue(dataNode, CrmProductInfo[].class);
            response.setResponseData(products == null ? List.of() : Arrays.asList(products));
        }
        return response;
    }
}
