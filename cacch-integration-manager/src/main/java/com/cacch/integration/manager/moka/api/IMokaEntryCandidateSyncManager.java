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
     * 同步统计结果 DTO
     */
    record MokaEntrySyncResult(
            int step1aTotal,
            int step1bDepartmentCodeUpdated,
            int step1cOfferFieldsUpdated
    ) {
    }
}
