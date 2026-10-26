package com.cacch.integration.manager.moka.api;

/**
 * Moka 待入职候选人同步编排接口（第一步：Moka → 本地快照 + 补全）
 *
 * <p>编排三阶段：
 * <ol>
 *     <li><strong>Step 1a</strong>：分页拉取 Moka list_by_condition → 4 表幂等 upsert</li>
 *     <li><strong>Step 1b</strong>：按 jobId 去重查职位详情 → 回写 moka_department_code</li>
 *     <li><strong>Step 1c</strong>：按 applicationId 批量查 Offer 扩展 → 回写 check_in_date / offer_department_code</li>
 * </ol></p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaEntryCandidateSyncManager {

    /**
     * 执行完整的第一步同步（1a → 1b → 1c）
     *
     * @return 同步统计结果
     */
    MokaEntrySyncResult syncEntryCandidates();

    /**
     * 历史薪资补数：回填 salary_number 为空的历史记录
     *
     * <p>不依赖 Moka list_by_condition 拉取范围，直接从本地快照定位缺口，
     * 调 Offer 扩展接口回填（复用 Step 1c 回写逻辑）。幂等可重复调用。</p>
     *
     * @return 薪资补数统计结果
     */
    MokaOfferSalaryBackfillResult backfillOfferSalary();

    /**
     * 同步统计结果 DTO
     */
    record MokaEntrySyncResult(
            int step1aTotal,
            int step1bDepartmentCodeUpdated,
            int step1cOfferFieldsUpdated
    ) {
    }

    /**
     * 历史薪资补数统计结果 DTO
     *
     * @param missingCount 缺口记录数（salary_number 为空且有 Offer 数据的记录）
     * @param updatedCount 成功回写记录数
     */
    record MokaOfferSalaryBackfillResult(
            int missingCount,
            int updatedCount
    ) {
    }
}
