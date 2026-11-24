package com.cacch.integration.service.crm.api;

import com.cacch.integration.integration.crm.client.dto.CrmProductQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmProductQueryResponse;

/**
 * 勤策 CRM 商品域服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmProductService {

    /**
     * 查询商品
     *
     * <p>查询条件全部可选；请求为空时按空条件查询第一页。</p>
     *
     * @param request 商品查询条件，可为空
     * @return 商品查询响应，response_data 为商品列表
     * @throws com.cacch.integration.common.exception.BizException return_code 非 0 或调用失败时抛出
     */
    CrmProductQueryResponse queryProducts(CrmProductQueryRequest request);
}
