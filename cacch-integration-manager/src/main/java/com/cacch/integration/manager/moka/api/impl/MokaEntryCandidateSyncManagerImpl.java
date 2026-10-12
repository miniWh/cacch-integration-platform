package com.cacch.integration.manager.moka.api.impl;

import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.entity.moka.MokaEntryApplicationDO;
import com.cacch.integration.manager.moka.api.IMokaEntryCandidateSyncManager;
import com.cacch.integration.service.moka.api.IMokaEntryCandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Moka 待入职候选人同步编排实现（第一步：Moka → 本地快照 + 补全）
 *
 * <p>编排三阶段：
 * <ol>
 *     <li><strong>Step 1a</strong>：分页拉取 Moka list_by_condition → 4 表幂等 upsert
 *         （Service 层统一事务）</li>
 *     <li><strong>Step 1b</strong>：按 jobId 去重查职位详情 → 回写 moka_department_code
 *         （Service 层独立 UPDATE，每批事务独立）</li>
 *     <li><strong>Step 1c</strong>：按 applicationId 批量查 Offer 扩展 → 回写 check_in_date / offer_department_code
 *         （Service 层独立 UPDATE，每批事务独立）</li>
 * </ol></p>
 *
 * <p>事务策略：Manager 层不包裹大事务，避免跨 HTTP 调用 + 长事务。
 * Step 1a 的事务由 Service.syncAndUpsert 控制（REQUIRED，120s），
 * Step 1b/1c 的 UPDATE 由 Service 层单独执行（每次 UPDATE 自动提交）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaEntryCandidateSyncManagerImpl implements IMokaEntryCandidateSyncManager {

    private static final String BIZ = "Moka待入职同步编排";

    private final IMokaEntryCandidateService entryCandidateService;

    @Override
    public MokaEntrySyncResult syncEntryCandidates() {
        String batch = MokaConstants.ENTRY_SYNC_BATCH_PREFIX
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        log.info("【{}】开始第一步同步, batch={}", BIZ, batch);

        // === Step 1a：快照入库 ===
        List<MokaEntryApplicationDO> applications = entryCandidateService.syncAndUpsert(batch);
        int step1aTotal = applications.size();

        if (applications.isEmpty()) {
            log.info("【{}】Step1a 返回空数据, 同步结束, batch={}", BIZ, batch);
            return new MokaEntrySyncResult(0, 0, 0);
        }

        // === Step 1b：职位详情补全 departmentCode ===
        int step1bUpdated = 0;
        try {
            step1bUpdated = entryCandidateService.enrichDepartmentCode(applications);
        } catch (Exception e) {
            log.info("【{}】Step1b 异常终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】Step1b 异常堆栈", BIZ, e);
        }

        // === Step 1c：Offer 扩展补全 checkInDate ===
        int step1cUpdated = 0;
        try {
            step1cUpdated = entryCandidateService.enrichOfferFields(applications);
        } catch (Exception e) {
            log.info("【{}】Step1c 异常终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】Step1c 异常堆栈", BIZ, e);
        }

        log.info("【{}】第一步同步完成, batch={}, 1a入库={}, 1b补全部门={}, 1c补全Offer={}",
                BIZ, batch, step1aTotal, step1bUpdated, step1cUpdated);

        return new MokaEntrySyncResult(step1aTotal, step1bUpdated, step1cUpdated);
    }
}
