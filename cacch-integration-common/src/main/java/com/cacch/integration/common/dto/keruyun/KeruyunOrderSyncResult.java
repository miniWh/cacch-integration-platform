package com.cacch.integration.common.dto.keruyun;

import com.cacch.integration.common.dto.batch.BatchProcessResult;
import lombok.Getter;
import lombok.Setter;

/**
 * 客如云订单同步结果
 *
 * <p>继承通用批量处理结果基类，成功/失败/跳过条数与明细由基类自动维护；
 * 本类只额外提供同步元信息与新增/更新拆分统计。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class KeruyunOrderSyncResult extends BatchProcessResult<KeruyunOrderSyncItem> {

    /**
     * 查询时间类型：FINISH_BUSI_DATE / OPEN_TIME / FINISH_TIME / SETTLE_TIME
     */
    private final String dateType;

    /**
     * 查询开始时间（原始入参格式）
     */
    private final String startDate;

    /**
     * 查询结束时间（原始入参格式）
     */
    private final String endDate;

    /**
     * 实际拉取的页数
     */
    @Setter
    private int pageCount;

    /**
     * 新增入库条数
     */
    private int insertedCount;

    /**
     * 更新入库条数
     */
    private int updatedCount;

    /**
     * 构造同步结果
     *
     * @param dateType  查询时间类型
     * @param startDate 查询开始时间
     * @param endDate   查询结束时间
     */
    public KeruyunOrderSyncResult(String dateType, String startDate, String endDate) {
        this.dateType = dateType;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * 成功回调：按写入动作自动累加新增/更新条数
     *
     * @param item 订单同步明细主体
     */
    @Override
    protected void onSuccess(KeruyunOrderSyncItem item) {
        if (item.getOperation() == KeruyunOrderSyncItem.Operation.UPDATED) {
            updatedCount++;
        } else {
            insertedCount++;
        }
    }
}
