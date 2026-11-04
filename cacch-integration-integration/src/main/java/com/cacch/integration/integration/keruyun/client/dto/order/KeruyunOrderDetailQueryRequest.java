package com.cacch.integration.integration.keruyun.client.dto.order;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

/**
 * 客如云订单详情查询请求体
 *
 * <p>对应接口 {@code POST /open/standard/order/queryDetail}，业务参数仅 orderId。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonPropertyOrder({"orderId"})
public class KeruyunOrderDetailQueryRequest {

    /**
     * 客如云订单 ID
     */
    private String orderId;

    /**
     * 构造请求体
     *
     * @param orderId 客如云订单 ID
     */
    public KeruyunOrderDetailQueryRequest(String orderId) {
        this.orderId = orderId;
    }
}
