package com.cacch.integration.common.dto.keruyun;

import lombok.Builder;
import lombok.Data;

/**
 * 客如云订单同步执行结果统计
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class KeruyunOrderSyncResult {

    /**
     * 查询时间类型：FINISH_BUSI_DATE / OPEN_TIME / FINISH_TIME / SETTLE_TIME
     */
    private String dateType;

    /**
     * 查询开始时间（原始入参格式）
     */
    private String startDate;

    /**
     * 查询结束时间（原始入参格式）
     */
    private String endDate;

    /**
     * 实际拉取的页数
     */
    private int pageCount;

    /**
     * 客如云返回的订单条数
     */
    private int scanned;

    /**
     * 新增入库条数
     */
    private int inserted;

    /**
     * 更新入库条数
     */
    private int updated;

    /**
     * 因关键字段缺失被跳过的条数
     */
    private int skipped;

    /**
     * 单条落库失败条数
     */
    private int failed;
}
