package com.cacch.integration.common.dto.keruyun;

import lombok.Data;

/**
 * 客如云订单详情同步明细主体
 *
 * <p>作为 {@link KeruyunOrderDetailSyncResult} 的泛型明细，承载业务定位所需的键信息。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderDetailSyncItem {

    /**
     * 客如云订单 ID
     */
    private String orderId;

    /**
     * 业务订单号
     */
    private String busiOrderNo;

    /**
     * 门店 ID
     */
    private String shopId;

    /**
     * 成功时落库的菜品明细条数；失败/跳过时为 null
     */
    private Integer itemCount;

    /**
     * 成功时落库的支付明细条数；失败/跳过时为 null
     */
    private Integer paymentCount;
}
