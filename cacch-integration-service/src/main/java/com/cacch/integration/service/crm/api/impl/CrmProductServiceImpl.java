package com.cacch.integration.service.crm.api.impl;

import com.cacch.integration.common.constant.crm.CrmProductConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.crm.client.CrmProductClient;
import com.cacch.integration.integration.crm.client.dto.CrmProductQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmProductQueryResponse;
import com.cacch.integration.service.crm.api.ICrmProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * 勤策 CRM 商品域服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmProductServiceImpl implements ICrmProductService {

    private final CrmProductClient crmProductClient;

    @Override
    public CrmProductQueryResponse queryProducts(CrmProductQueryRequest request) {
        CrmProductQueryRequest query = request != null ? request : new CrmProductQueryRequest();
        if (query.getPageNumber() == null || query.getPageNumber() < 1) {
            query.setPageNumber(CrmProductConstants.DEFAULT_PAGE_NUMBER);
        }
        try {
            CrmProductQueryResponse response = crmProductClient.queryProducts(query);
            if (!response.isSuccess()) {
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "查询商品失败, return_code=" + response.getReturnCode()
                                + ", return_msg=" + response.getReturnMsg());
            }
            return response;
        } catch (BizException e) {
            log.error("【CrmProduct】查询商品终止, reason={}", e.getMessage());
            throw e;
        } catch (RestClientException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "查询商品调用失败: " + e.getMessage(), e);
        }
    }
}
