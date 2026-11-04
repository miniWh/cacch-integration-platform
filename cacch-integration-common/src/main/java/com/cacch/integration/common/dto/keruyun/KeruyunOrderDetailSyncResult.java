package com.cacch.integration.common.dto.keruyun;

import com.cacch.integration.common.dto.batch.BatchProcessResult;
import lombok.Getter;
import lombok.Setter;

/**
 * 客如云订单详情同步结果
 *
 * <p>继承通用批量处理结果基类，成功/失败/跳过条数与明细由基类自动维护；
 * 本类只额外提供同步元信息。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class KeruyunOrderDetailSyncResult extends BatchProcessResult<KeruyunOrderDetailSyncItem> {

    /**
     * 本次请求的处理上限（截断后的实际值）
     */
    private final int limit;

    /**
     * 本次实际处理的待同步订单条数（未超 limit 时等于剩余待同步量）
     * -- SETTER --
     * 记录本次实际处理的待同步订单条数
     */
    @Setter
    private int pendingTotal;

    /**
     * 构造同步结果
     *
     * @param limit 本次处理上限
     */
    public KeruyunOrderDetailSyncResult(int limit) {
        this.limit = limit;
    }

}
