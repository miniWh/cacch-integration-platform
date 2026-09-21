package com.cacch.integration.manager.moka.api;

/**
 * Moka 人员推送编排接口 —— 从 Moka 人员中间表批量推送到 Moka 开放平台
 *
 * <p>典型调用链：Controller → pushToMoka() → IMokaPersonService.listBySyncStatusIn()
 * （查 PENDING+SYNC_FAILED 待推送记录）→ 内存分批（≤ 100 条 / 批）→
 * MokaUserClient.syncUserInfo()（HTTP 推送）→
 * IMokaPersonService.batchUpdateSyncStatus()（按推送结果回写状态）。</p>
 *
 * <p>事务策略：Manager 层不在外层包裹 {@code @Transactional}。
 * <ul>
 *     <li>HTTP 调用（MokaUserClient）禁止加事务</li>
 *     <li>批量状态回写由 {@code IMokaPersonService.batchUpdateSyncStatus}
 *     内部显式声明事务（PROPAGATION.REQUIRED, timeout=120s）</li>
 * </ul>
 * </p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaPersonPushManager {

    /**
     * 推送 Moka 人员中间表 → Moka 开放平台
     *
     * <p>执行流程：
     * <ol>
     *     <li>查询 {@code moka_sync_status IN (0, 2)} 的待推送记录（PENDING + SYNC_FAILED）</li>
     *     <li>按每批 ≤ 100 条切分</li>
     *     <li>每批字段映射 → {@code MokaUserSyncRequest} → 调用 Moka syncInfo API</li>
     *     <li>Moka API 整批成功 → 回写 sync_status=1（SYNCED）；整批失败 → 回写 sync_status=2（SYNC_FAILED）</li>
     *     <li>累计 syncedCount / syncFailedCount / dbUpdateFailedCount 等指标</li>
     * </ol>
     *
     * <p>幂等性：Moka syncInfo API 以手机号为唯一键做 upsert，
     * 同一批次可重复推送；DB 状态字段 lastSyncTime / lastSyncResult 每次覆盖。</p>
     *
     * @return 推送结果（总读取数 / 批次数 / API 整体成功标记 / 同步成功数 / 同步失败数 / DB 更新失败数）
     */
    MokaPersonPushResult pushToMoka();

    /**
     * 推送执行结果
     *
     * @param totalRead           从中间表读取的待推送记录总数
     * @param batchCount          实际推送的批次数（每批 ≤ 100 条）
     * @param mokaApiSuccess      Moka API 整体调用是否成功（所有批次都成功才为 true）
     * @param syncedCount         推送成功并更新为 SYNCED 的条数
     * @param syncFailedCount     推送失败并更新为 SYNC_FAILED 的条数（含 API 失败 + HTTP 异常）
     * @param dbUpdateFailedCount DB 状态回写失败的条数
     */
    record MokaPersonPushResult(int totalRead, int batchCount, boolean mokaApiSuccess,
                                int syncedCount, int syncFailedCount,
                                int dbUpdateFailedCount) {
    }
}
