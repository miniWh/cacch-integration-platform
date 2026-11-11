package com.cacch.integration.manager.moka.api;

import java.util.List;

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
     * 将本地 PG 表中的部门推送到 Moka 开放平台（前置处理待删除部门 + PUT 全量同步）
     *
     * <p>执行流程：
     * <ol>
     *     <li>前置删除步骤：查询 {@code moka_sync_status=3}（待删除）的部门，
     *     经孤儿校验（存在未删除子部门的跳过）与自底向上排序后调
     *     Moka DELETE /api-platform/v1/departments 标记删除；成功置 4（已删除），
     *     失败保留 3 待下轮重试。{@code moka.delete-email} 未配置时本步骤整体跳过。</li>
     *     <li>PUT 全量同步：查询本地 PG 全量部门（排除状态 3/4），
     *     调 Moka PUT /api-platform/v2/departments 全量推送。</li>
     * </ol>
     *
     * <p>同步语义：Moka PUT 以 departmentCode 为主键，本次传入的 → 新增/更新；
     * 系统有但本次未传的 → 标记删除。因此推送范围必须是全量（排除待删/已删），
     * 否则会把 Moka 侧已存在但不在推送列表里的部门标记删除。</p>
     *
     * <p>moka_sync_status 更新策略：
     * <ul>
     *     <li>PUT 返回 code=0 → 全部推送部门置 1（SYNCED，记录 moka_sync_time）</li>
     *     <li>PUT 调用异常 / code≠0 → 全部推送部门置 2（SYNC_FAILED）</li>
     *     <li>DELETE 成功 → 待删部门置 4（DELETED，记录 moka_delete_time）</li>
     * </ul>
     *
     * @return 推送执行结果
     */
    MokaDeptPushResult pushToMoka();

    /**
     * 手动触发部门删除 —— 标记指定部门为待删除并立即执行 Moka 删除编排
     *
     * <p>入口模式：
     * <ul>
     *     <li>departmentCodes 非空（手动模式）：去重后逐条校验标记</li>
     *     <li>departmentCodes 为 null/空（自动模式）：查询本地 iHR 快照中
     *     {@code department_status='DISABLE'}（停用）的部门编码全集作为待删除目标</li>
     * </ul>
     *
     * <p>执行流程：
     * <ol>
     *     <li>部门编码校验（逐条，单条失败不阻断）：
     *     本地不存在的计 notFound；状态已是 4（已删除）的跳过标记计 alreadyDeleted；
     *     状态 0/1/2 的置 3（待删除）；状态已是 3 的无需重复标记</li>
     *     <li>立即执行删除编排（与 pushToMoka 前置删除步骤同一套逻辑，处理全部状态 3 部门）：
     *     {@code moka.delete-email} 未配置时跳过；孤儿校验（存在未删除子部门的跳过）；
     *     自底向上排序后调 Moka DELETE /api-platform/v1/departments；成功置 4（记录 moka_delete_time），
     *     失败保留 3 待下轮重试</li>
     * </ol>
     *
     * @param departmentCodes 待删除部门编码列表，可为 null/空（触发自动模式）
     * @return 手动删除执行结果
     */
    MokaManualDeleteResult deleteDepartments(List<String> departmentCodes);

    /**
     * 手动删除执行结果
     *
     * @param requestCount        本次参与校验的部门编码条数（手动模式=去重后传入数；自动模式=iHR 停用部门数）
     * @param notFoundCount       本地表中不存在的编码条数
     * @param alreadyDeletedCount 状态已是 4（已删除）而跳过标记的条数
     * @param markedCount         本次新标记为 3（待删除）的条数
     * @param deletePendingCount  删除编排命中的待删除（状态 3）部门总数（含此前差集标记的历史待删部门）
     * @param deleteSkippedCount  因存在未删除子部门（孤儿校验）而跳过删除的条数
     * @param deleteSuccess       Moka DELETE API 整体是否成功；
     *                            无待删除部门、未配置 delete-email 或全部被孤儿校验跳过时为 null（未调用）
     * @param deletedCount        Moka 删除成功后本地置 4（DELETED，记录 moka_delete_time）的条数
     *                            （deleteSuccess 非 true 时为 0）
     * @param autoResolved        true=自动模式（departmentCodes 为空，按 iHR 停用部门执行）；
     *                            false=手动模式（按传入编码执行）
     */
    record MokaManualDeleteResult(int requestCount, int notFoundCount, int alreadyDeletedCount,
                                  int markedCount, int deletePendingCount, int deleteSkippedCount,
                                  Boolean deleteSuccess, int deletedCount, boolean autoResolved) {
    }

    /**
     * 推送执行结果
     *
     * @param totalPushed         查询出的待推送部门总数（已排除待删除/已删除状态）
     * @param syncedToMoka        本次推送到 Moka 的条数（等于 totalPushed，因为是全量推送）
     * @param mokaApiSuccess      Moka PUT API 调用是否成功（true = code=0，false = 异常或 code≠0）
     * @param newOnMoka           Moka 侧新增数量（API 返回；mokaApiSuccess=false 时为 null）
     * @param updatedOnMoka       Moka 侧更新数量（API 返回；mokaApiSuccess=false 时为 null）
     * @param deletedOnMoka       Moka 侧标记删除数量（API 返回；mokaApiSuccess=false 时为 null）
     * @param syncedCount         本地 moka_sync_status 更新为 1（SYNCED）的条数
     *                            （Moka API 成功时才会有值，失败时为 0）
     * @param syncFailedCount     本地 moka_sync_status 更新为 2（SYNC_FAILED）的条数
     *                            （Moka API 失败时才会有值，成功时为 0）
     * @param dbUpdateFailedCount DB 状态更新自身抛异常的条数（逐条 try-catch 累计）
     * @param deletePendingCount  前置删除步骤命中的待删除（状态 3）部门总数
     * @param deleteSkippedCount  因存在未删除子部门（孤儿校验）而跳过删除的条数
     * @param deleteSuccess       Moka DELETE API 整体是否成功；
     *                            无待删除部门、未配置 delete-email 或全部被孤儿校验跳过时为 null（未调用）
     * @param deletedCount        Moka 删除成功后本地置 4（DELETED）的条数（deleteSuccess 非 true 时为 0）
     */
    record MokaDeptPushResult(int totalPushed, int syncedToMoka,
                              boolean mokaApiSuccess,
                              Integer newOnMoka, Integer updatedOnMoka, Integer deletedOnMoka,
                              int syncedCount, int syncFailedCount, int dbUpdateFailedCount,
                              int deletePendingCount, int deleteSkippedCount,
                              Boolean deleteSuccess, int deletedCount) {
    }
}
