package com.cacch.integration.manager.moka.api;

/**
 * Moka 待入职候选人 → iHR 待入职 推送编排接口
 *
 * <p>职责：读取 PG 快照 4 表 JOIN 结果 → 字段映射 → 逐条推送 iHR → 更新同步状态。
 * 跨聚合编排：Moka 本地快照（dao）+ iHR 新增待入职（service）+ 同步状态（dao）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaEntryPushManager {

    /**
     * 执行 Moka 待入职 → iHR 待入职 推送
     *
     * <p>流程：
     * <ol>
     *     <li>读取同步状态为 PENDING/FAILED 的候选人（4 表 JOIN + ihr_department）</li>
     *     <li>逐条前置校验（部门/手机号/入职日期缺失 → SKIPPED）</li>
     *     <li>字段映射 → 调 iHR 新增待入职接口</li>
     *     <li>更新同步状态（SUCCESS / FAILED / SKIPPED）</li>
     * </ol></p>
     *
     * <p>事务策略：不在外层包裹 @Transactional。HTTP 调用与 DB 状态回写分离，
     * 每条推送完独立 UPDATE 同步状态，失败不阻断。</p>
     *
     * @param statusList 要推送的同步状态列表（如 ["PENDING","FAILED"]）
     * @param limit      单次读取上限
     * @return 推送汇总结果
     */
    MokaEntryPushSummary pushToIhr(java.util.List<String> statusList, int limit);

    /**
     * 推送汇总 —— 成功/失败/跳过计数
     */
    record MokaEntryPushSummary(int total, int success, int failed, int skipped) {
        /**
         * 追加一条结果
         */
        public MokaEntryPushSummary add(String resultType) {
            return switch (resultType) {
                case "success" -> new MokaEntryPushSummary(total + 1, success + 1, failed, skipped);
                case "failed" -> new MokaEntryPushSummary(total + 1, success, failed + 1, skipped);
                case "skipped" -> new MokaEntryPushSummary(total + 1, success, failed, skipped + 1);
                default -> this;
            };
        }
    }
}
