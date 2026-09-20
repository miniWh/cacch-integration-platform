package com.cacch.integration.manager.moka.api;

/**
 * Moka 部门推送到 Moka 开放平台 —— 编排接口
 *
 * <p>调用链：Controller → 本 Manager → IMokaDepartmentService（查本地PG）
 * + IMokaOrgService（调 Moka API）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaDepartmentPushManager {

    /**
     * 将本地 PG 表中待同步的部门全量推送到 Moka 开放平台
     *
     * <p>查询范围：{@code moka_sync_status IN (0, 2)} —— PENDING（从未推送）或 FAILED（上次推送失败）。</p>
     *
     * <p>同步语义：Moka PUT /api-platform/v2/departments 以 departmentCode 为主键，
     * 本次传入的 → 新增/更新；系统有但本次未传的 → 标记删除。
     * 因此推送范围必须是**全量**（而非仅待同步的），否则会把 Moka 侧已存在但不在待同步列表里的部门标记删除。</p>
     *
     * <p>moka_sync_status 更新策略：
     * <ul>
     *     <li>Moka API 返回 code=0 → 全部待推送部门置 1（SYNCED）</li>
     *     <li>Moka API 调用异常 / code≠0 → 全部待推送部门置 2 (SYNC_FAILED)</li>
     * </ul>
     *
     * @param operatorEmail 操作人邮箱（可选，写入 Moka 侧日志）；null 时传 "system@cacch.com"
     * @return 推送执行结果
     */
    MokaDeptPushResult pushToMoka(String operatorEmail);

    /**
     * 推送执行结果
     *
     * @param totalPushed       查询出的待推送部门总数
     * @param syncedToMoka      本次推送到 Moka 的条数（等于 totalPushed，因为是全量推送）
     * @param newOnMoka         Moka 侧新增数量（API 返回）
     * @param updatedOnMoka     Moka 侧更新数量（API 返回）
     * @param deletedOnMoka     Moka 侧标记删除数量（API 返回，本地无对应部门）
     * @param syncStatusSuccess 本地 moka_sync_status 成功更新为 1 的条数
     */
    record MokaDeptPushResult(int totalPushed, int syncedToMoka,
                              Integer newOnMoka, Integer updatedOnMoka, Integer deletedOnMoka,
                              int syncStatusSuccess) {
    }
}
