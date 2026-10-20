package com.cacch.integration.manager.moka.api;

import java.util.List;

import com.cacch.integration.entity.moka.MokaPersonStatusDO;

/**
 * Moka 人员入离职状态同步编排接口
 *
 * <p>职责：将 t_integration_moka_person 中间表中的在职 / 离职人员，
 * 按 staffNo（= employee_no）关联 persondetail 取入职 / 离职日期，
 * 调用 Moka hired / resign 申请状态接口完成状态同步，并回写状态表
 * {@code t_integration_moka_person_status}。</p>
 *
 * <p>与 REQ-MOKA-001 人员主数据链路（sync-from-ihr / push-to-moka）并行，互不影响。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaPersonStatusSyncManager {

    /**
     * 执行人员入离职状态同步
     *
     * <p>执行流程：
     * <ol>
     *     <li>查询入职 / 离职待处理集合（状态行缺失或状态 ∈ {PENDING, SYNC_FAILED}）</li>
     *     <li>按工号批量关联 persondetail，取 contractBeginDate / probationEndDate / leaveDate</li>
     *     <li>逐条解析 applicationId（机制待定，为空则跳过且不写状态行）</li>
     *     <li>在职：调 Moka hired 接口（hiredAt=contractBeginDate，probation=整月差值 0-6）</li>
     *     <li>离职：调 Moka resign 接口（time=leaveDate，detail=固定文案）</li>
     *     <li>逐条回写状态表（upsert，幂等可重跑；单条失败不阻断其余）</li>
     * </ol></p>
     *
     * @return 同步执行结果计数
     */
    MokaPersonStatusSyncResult pushStatusToMoka();

    /**
     * 同步执行结果
     *
     * @param totalRead            读取的待处理记录总数（入职 + 离职）
     * @param hiredSuccess         入职同步成功条数（Moka 成功且状态行回写成功）
     * @param hiredFailed          入职同步失败条数（Moka 调用失败，含业务失败与异常）
     * @param resignSuccess        离职同步成功条数（Moka 成功且状态行回写成功）
     * @param resignFailed         离职同步失败条数（Moka 调用失败，含业务失败与异常）
     * @param skippedNoApplication applicationId 为空被跳过的条数（不写状态行）
     * @param dbUpdateFailed       状态行回写失败条数（含 Moka 成功后回写失败）
     */
    record MokaPersonStatusSyncResult(int totalRead,
                                      int hiredSuccess,
                                      int hiredFailed,
                                      int resignSuccess,
                                      int resignFailed,
                                      int skippedNoApplication,
                                      int dbUpdateFailed) {
    }
}
