package com.cacch.integration.dto.keruyun.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 客如云订单同步批次触发请求
 *
 * <p>指定基准日期后，Controller 内部执行两步编排：
 * ① 检查控制表，缺失则新建 PENDING；② 取所有 PENDING 任务逐条异步执行。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderSyncTaskExecuteRequest {

    /**
     * 基准日期（按天）
     *
     * <p>不传时默认取昨天（LocalDate.now().minusDays(1)）。</p>
     */
    private LocalDate anchorDate;

    /**
     * 时间类型过滤
     *
     * <p>不传时对所有 dateType 执行（FINISH_BUSI_DATE / OPEN_TIME / FINISH_TIME / SETTLE_TIME）。</p>
     */
    private String dateType;
}
