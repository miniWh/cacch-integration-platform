package com.cacch.integration.common.dto.keruyun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 客如云订单同步任务批次执行结果
 *
 * <p>由 {@link com.cacch.integration.manager.keruyun.api.IKeruyunOrderSyncTaskManager#executeBatch}
 * 返回，描述本次触发时新建了多少 PENDING 任务、待执行任务有多少、以及任务 ID 列表供后续查询。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeruyunOrderSyncBatchResult {

    /**
     * 本次新创建的 PENDING 任务数量
     */
    private int createdTaskCount;

    /**
     * 本次发现的待执行（PENDING + 可重试的 FAILED/PARTIAL）任务总数
     */
    private int pendingTaskCount;

    /**
     * 本次触发执行的任务 ID 列表（异步执行中）
     */
    private List<Long> taskIds;

    /**
     * 执行概览描述
     */
    private String message;
}
