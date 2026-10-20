package com.cacch.integration.manager.moka.api;

/**
 * Moka 候选人入离职状态同步编排接口
 *
 * <p>职责：将 t_integration_moka_entry_candidate_base（待入职候选人快照）中
 * 有记录的员工，按 contact_phone = moka_contact_phone 关联
 * t_integration_moka_person，再按 employee_status（IN_SERVICE/QUIT）
 * 触发 Moka ATS 申请状态接口 hired / resign。</p>
 *
 * <p>驱动边界：
 * <ul>
 *     <li>INNER JOIN candidate_base 天然过滤掉「无候选人记录」的员工，
 *     确保只有在 Moka ATS 待入职快照中有对应 application_id 的候选人才处理</li>
 *     <li>application_id 由 candidate_base JOIN 直接取，无需额外解析</li>
 *     <li>与 REQ-MOKA-001 人员主数据链路（sync-from-ihr / push-to-moka）并行，互不影响</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaPersonStatusSyncManager {

    /**
     * 执行候选人入离职状态同步
     *
     * <p>执行流程：
     * <ol>
     *     <li>INNER JOIN candidate_base 查询入职 / 离职待处理集合（天然过滤无候选人）</li>
     *     <li>按工号批量关联 persondetail，取 contractBeginDate / probationEndDate / leaveDate</li>
     *     <li>在职：调 Moka hired 接口（hiredAt=contractBeginDate，probation=整月差值 0-6）</li>
     *     <li>离职：调 Moka resign 接口（time=leaveDate，detail=固定文案）</li>
     *     <li>逐条回写 t_integration_moka_person_status（upsert，幂等可重跑）</li>
     * </ol></p>
     *
     * @return 同步执行结果计数
     */
    MokaPersonStatusSyncResult pushStatusToMoka();

    /**
     * 同步执行结果
     *
     * @param totalRead      读取的待处理记录总数（入职 + 离职，已 INNER JOIN 过滤无候选人）
     * @param hiredSuccess   入职同步成功条数（Moka 成功且状态行回写成功）
     * @param hiredFailed    入职同步失败条数（Moka 调用失败，含业务失败与异常）
     * @param resignSuccess  离职同步成功条数（Moka 成功且状态行回写成功）
     * @param resignFailed   离职同步失败条数（Moka 调用失败，含业务失败与异常）
     * @param dbUpdateFailed 状态行回写失败条数（含 Moka 成功后回写失败）
     */
    record MokaPersonStatusSyncResult(int totalRead,
                                      int hiredSuccess,
                                      int hiredFailed,
                                      int resignSuccess,
                                      int resignFailed,
                                      int dbUpdateFailed) {
    }
}
