package com.cacch.integration.service.keruyun.api;

import com.cacch.integration.entity.keruyun.KeruyunOrderSyncTaskDO;

import java.time.LocalDate;
import java.util.List;

/**
 * 客如云订单同步任务控制表服务接口
 *
 * <p>负责控制表的 CRUD、幂等创建、状态机流转（CAS）与进度更新，
 * 由 Manager 层编排调用，不直接操作客如云接口。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunOrderSyncTaskService {

    /**
     * 按主键查询任务
     *
     * @param id 任务主键，不可为空
     * @return 任务 DO，不存在返回 null
     */
    KeruyunOrderSyncTaskDO getById(Long id);

    /**
     * 查所有 PENDING 状态任务（按 sync_date 升序）
     *
     * @return 任务列表，无记录返回空列表
     */
    List<KeruyunOrderSyncTaskDO> findAllPending();

    /**
     * 查所有可重试的 FAILED / PARTIAL 状态任务
     *
     * @return 任务列表
     */
    List<KeruyunOrderSyncTaskDO> findAllRetryable();

    /**
     * 创建单天 PENDING 任务（幂等）
     *
     * <p>计算窗口 = syncDate 当天 00:00:00 ~ 23:59:59；
     * 唯一键 (sync_date, date_type) 冲突时返回 null，
     * 表示该任务已存在或已被其他执行器抢先创建。</p>
     *
     * @param syncDate 同步日期（按天），不可为空
     * @param dateType 查询时间类型，不可为空
     * @return 新创建的任务 DO，唯一键冲突返回 null
     */
    KeruyunOrderSyncTaskDO createTask(LocalDate syncDate, String dateType);

    /**
     * 批量创建 PENDING 任务（幂等）
     *
     * <p>遍历日期区间 + 所有 dateType，逐个调用 {@link #createTask}；
     * 已存在的记录跳过，返回实际新建数量。</p>
     *
     * @param from 起始日期（含），不可为空
     * @param to   结束日期（含），不可为空
     * @return 本次新创建的 PENDING 任务数量
     */
    int createMissingTasks(LocalDate from, LocalDate to);

    /**
     * CAS 状态流转
     *
     * <p>仅当当前 status 等于 expectedStatus 且 version 匹配时，
     * 更新为 newStatus 并 version +1。用于 IN_PROGRESS / SUCCESS / FAILED 的互斥更新。</p>
     *
     * @param id             任务主键
     * @param expectedStatus 期望的当前状态（CAS 前置条件）
     * @param newStatus      目标状态
     * @return true 表示更新成功；false 表示被其他执行器抢先或任务不存在
     */
    boolean transitionStatus(Long id, String expectedStatus, String newStatus);

    /**
     * 更新执行进度（断点续传字段 + 统计字段）
     *
     * @param id           任务主键
     * @param lastPage     已拉取到的最后页码
     * @param totalPage    总页数（首次执行拉取到第一页后回填，可为 null）
     * @param successCount 当前累计落库成功数
     * @param failureCount 当前累计落库失败数
     */
    void updateProgress(Long id, int lastPage, Integer totalPage, int successCount, int failureCount);

    /**
     * 标记任务终态（SUCCESS / FAILED / PARTIAL）
     *
     * <p>同时写入 synced_at、error_message（截断至 500 字符）与最终统计。</p>
     *
     * @param id           任务主键
     * @param finalStatus  终态：SUCCESS / FAILED / PARTIAL
     * @param errorMessage 失败原因，可为 null（SUCCESS 时通常为 null）
     */
    void markComplete(Long id, String finalStatus, String errorMessage);

    /**
     * 重试：将 FAILED / PARTIAL 任务重置为 PENDING
     *
     * <p>重置 last_page=0、统计字段归零、retry_count +1。</p>
     *
     * @param id 任务主键
     * @return true 表示重置成功；false 表示任务不存在或已在 IN_PROGRESS 中
     */
    boolean retry(Long id);
}
