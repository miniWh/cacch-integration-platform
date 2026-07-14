package com.cacch.integration.manager.crm.api;

import com.cacch.integration.common.dto.crm.CrmOrderCollectResult;

/**
 * CRM 订单采集编排（阶段一）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmOrderCollectManager {

    /**
     * 按默认当天（Asia/Shanghai）时间窗采集订单并拉取明细，再补拉失败明细
     *
     * @return 采集统计
     */
    CrmOrderCollectResult collectToday();

    /**
     * 按指定时间窗采集（入参可为 yyyy-MM-dd HH:mm:ss 或毫秒时间戳）
     *
     * @param beginTime 起始时间，不可为空
     * @param endTime   结束时间，可空
     * @return 采集统计
     */
    CrmOrderCollectResult collectByModifyTime(String beginTime, String endTime);
}
