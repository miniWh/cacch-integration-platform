package com.cacch.integration.common.dto.crm;

import lombok.Builder;
import lombok.Data;

/**
 * CRM 订单采集执行结果统计
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class CrmOrderCollectResult {

    /**
     * 查询时间窗起始（毫秒）
     */
    private String beginEpochMilli;

    /**
     * 查询时间窗结束（毫秒）
     */
    private String endEpochMilli;

    /**
     * CRM 本轮扫描到的订单数
     */
    private int scannedFromCrm;

    /**
     * 新插入主表数
     */
    private int orderInserted;

    /**
     * 主表已存在跳过数
     */
    private int orderSkipped;

    /**
     * 明细拉取成功订单数
     */
    private int detailFetchSuccess;

    /**
     * 明细拉取失败订单数
     */
    private int detailFetchFailed;

    /**
     * 新插入明细行数
     */
    private int detailInserted;

    /**
     * 明细已存在跳过行数
     */
    private int detailSkipped;

    /**
     * 补拉扫描订单数
     */
    private int retryScanned;
}
