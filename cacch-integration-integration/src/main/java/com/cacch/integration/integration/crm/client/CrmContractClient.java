package com.cacch.integration.integration.crm.client;

import com.cacch.integration.common.config.crm.CrmProperties;
import com.cacch.integration.common.constant.crm.CrmContractLogConstants;
import com.cacch.integration.common.constant.crm.CrmContractPathConstants;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddRequest;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddResponse;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * 勤策 CRM 合同域 OpenAPI 客户端
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
public class CrmContractClient extends AbstractCrmClient {

    public CrmContractClient(RestTemplateHttpClient httpClient, CrmProperties crmProperties) {
        super(httpClient, crmProperties);
    }

    /**
     * 新增销售合同
     *
     * @param request 新增销售合同请求体，入参为 camelCase
     * @return 新增销售合同响应，response_data 为 JSON 节点（含新增合同 ID）
     * @throws RestClientException HTTP 调用失败或响应解析异常时抛出
     */
    public CrmContractAddResponse addContract(CrmContractAddRequest request) {
        CrmOpenApiResponse raw = post(crmProperties.getBaseUrl(),
                CrmContractPathConstants.CONTRACT_ADD_PATH,
                request, CrmContractLogConstants.LOG_BIZ, "新增销售合同");

        CrmContractAddResponse response = new CrmContractAddResponse();
        response.setReturnCode(raw.getReturnCode());
        response.setReturnMsg(raw.getReturnMsg());
        response.setMsgId(raw.getMsgId());
        response.setResponseData(raw.getResponseData());
        return response;
    }
}
