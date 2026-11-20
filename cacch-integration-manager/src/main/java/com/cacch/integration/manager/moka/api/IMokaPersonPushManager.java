package com.cacch.integration.manager.moka.api;

/**
 * Moka 人员推送编排接口 —— 从 Moka 人员中间表批量推送到 Moka 开放平台
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaPersonPushManager {

    /**
     * 推送 Moka 人员中间表 → Moka 开放平台（前置全量 V3 对账）
     *
     * <p>对账失败时抛 {@link com.cacch.integration.common.exception.BizException} 并发送企微 Webhook 告警，不执行推送。</p>
     *
     * @return 推送结果
     */
    MokaPersonPushResult pushToMoka();

    /**
     * 推送执行结果
     *
     * @param reconcileFetched     V3 拉取的用户条数
     * @param reconcileUpdated     本地对账更新条数
     * @param roleGateSkippedCount 角色门禁跳过条数（status=4）
     * @param totalRead            待推送读取数（门禁前）
     * @param batchCount           实际推送批次数
     * @param mokaApiSuccess       API 是否全部成功
     * @param syncedCount          推送成功条数
     * @param syncFailedCount      推送失败条数
     * @param dbUpdateFailedCount  DB 回写失败条数
     */
    record MokaPersonPushResult(int reconcileFetched, int reconcileUpdated, int roleGateSkippedCount,
                                int totalRead, int batchCount, boolean mokaApiSuccess,
                                int syncedCount, int syncFailedCount,
                                int dbUpdateFailedCount) {
    }
}