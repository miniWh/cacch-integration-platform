package com.cacch.integration.common.dto.keruyun;

import lombok.Data;

/**
 * 客如云订单同步明细主体
 *
 * <p>作为 {@link KeruyunOrderSyncResult} 的泛型明细，承载业务定位所需的键信息。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderSyncItem {

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
     * 来源页码
     */
    private Integer pageNum;

    /**
     * 成功时的写入动作：新增或更新；失败/跳过时为 null
     */
    private Operation operation;

    /**
     * 订单写入动作
     */
    public enum Operation {

        /**
         * 新增入库
         */
        INSERTED,

        /**
         * 更新已有记录
         */
        UPDATED
    }
}
