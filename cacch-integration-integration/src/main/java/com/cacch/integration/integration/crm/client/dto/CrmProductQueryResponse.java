package com.cacch.integration.integration.crm.client.dto;

import java.util.List;

/**
 * 勤策查询商品响应
 *
 * <p>统一信封字段见 {@link CrmBaseResponse}，response_data 为商品列表。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public class CrmProductQueryResponse extends CrmBaseResponse<List<CrmProductInfo>> {
}
