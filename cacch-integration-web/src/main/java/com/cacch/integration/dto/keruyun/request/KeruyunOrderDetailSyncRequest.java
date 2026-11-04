package com.cacch.integration.dto.keruyun.request;

import lombok.Data;

/**
 * 客如云订单详情批量同步请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderDetailSyncRequest {

    /**
     * 单次处理订单数上限；不传默认 200，最大 1000，超过按 1000 截断
     */
    private Integer limit;
}
