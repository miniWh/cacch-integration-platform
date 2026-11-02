package com.cacch.integration.manager.keruyun.api;

import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncBatchResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客如云订单同步任务编排接口
 *
 * <p>负责「检查缺失 → 新建 PENDING → 逐条执行」的批量执行编排，
 * 并提供单任务的断点续传执行入口。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunOrderSyncTaskManager {

    /**
     * 执行同步批次（两步编排）
     *
     * <p>1. 确保指定日期范围内的控制表记录存在（不存在则创建 PENDING）
     * 2. 取所有 PENDING + 可重试任务，逐条触发异步执行
     * 3. 立即返回 BatchResult，不等待执行完成
     *
     * @param anchorDate 基准日期，null 时默认取昨天（LocalDate.now().minusDays(1)）
     * @param dateType   时间类型，null 时对所有 dateType 执行
     * @return 批次执行结果（新建数量、待执行数量、任务 ID 列表）
     */
    KeruyunOrderSyncBatchResult executeBatch(LocalDate anchorDate, String dateType);

    /**
     * 执行单个任务（供异步线程池调用，内部含断点续传逻辑）
     *
     * @param taskId 任务主键，不可为空
     */
    void executeSingleTask(Long taskId);

    /**
     * 手动触发跨天同步（单任务）
     *
     * <p>dateType 固定为 OPEN_TIME；以 startTime 的日期作为 sync_date，
     * 若该 (sync_date, OPEN_TIME) 已存在则重置查询窗口并重新执行，否则新建任务。
     * 立即返回 BatchResult，异步执行不阻塞。</p>
     *
     * @param startTime 查询窗口起始时间，不可为空
     * @param endTime   查询窗口结束时间，必须晚于 startTime
     * @return 批次执行结果（含任务 ID）
     */
    KeruyunOrderSyncBatchResult executeManualBatch(LocalDateTime startTime, LocalDateTime endTime);
}
