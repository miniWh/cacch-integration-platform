package com.cacch.integration.dto.crm.request;

import lombok.Data;

/**
 * 手动触发 CRM 订单采集请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class CrmOrderCollectRequest {

    /**
     * 起始 modify_time：yyyy-MM-dd HH:mm:ss 或毫秒时间戳；为空则采集当天
     */
    private String beginTime;

    /**
     * 结束 modify_time：可空
     */
    private String endTime;
}
