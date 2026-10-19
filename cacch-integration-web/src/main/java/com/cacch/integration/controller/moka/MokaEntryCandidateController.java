package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.manager.moka.api.IMokaEntryCandidateSyncManager;
import com.cacch.integration.manager.moka.api.IMokaEntryCandidateSyncManager.MokaEntrySyncResult;
import com.cacch.integration.manager.moka.api.IMokaEntryPushManager;
import com.cacch.integration.manager.moka.api.IMokaEntryPushManager.MokaEntryPushSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Moka 待入职候选人同步/推送 REST 接口
 *
 * <p>两步流程：
 * <ol>
 *     <li>{@code POST /sync}  —— Step 1：Moka → PG（快照入库 + 职位/Offer 补全）</li>
 *     <li>{@code POST /push-ihr} —— Step 2：PG → iHR（逐条推送新增待入职）</li>
 * </ol></p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/moka/entry-candidates")
@RequiredArgsConstructor
public class MokaEntryCandidateController {

    private final IMokaEntryCandidateSyncManager syncManager;
    private final IMokaEntryPushManager pushManager;

    /**
     * Moka 待入职候选人 -- Step 1: Moka &rarr; PG 快照
     *
     * <p>分页拉取 Moka list_by_condition，4 表幂等 upsert，
     * 补全职位部门编码（Step 1b）和 Offer 入职日期（Step 1c）。</p>
     *
     * @return 同步汇总（totalApplications / successCount / failedCount）
     */
    @PostMapping("/sync")
    public Result<MokaEntrySyncResult> sync() {
        log.info("【MokaEntryCandidateController】收到 Step 1 同步请求");
        MokaEntrySyncResult result = syncManager.syncEntryCandidates();
        return Result.success(result);
    }

    /**
     * Moka 待入职候选人 -- Step 2: PG &rarr; iHR 待入职
     *
     * <p>读取同步状态为 PENDING/FAILED/SKIPPED 的候选人，
     * 逐条校验（部门/手机号/入职日期）→ 字段映射 → 调 iHR 新增待入职接口 → 更新同步状态。</p>
     *
     * @param statusList 推送状态筛选（可选，默认 PENDING/FAILED/SKIPPED）
     * @param limit      单次读取上限（可选，默认 50）
     * @return 推送汇总（total / success / failed / skipped）
     */
    @PostMapping("/push-ihr")
    public Result<MokaEntryPushSummary> pushToIhr(
            @RequestParam(value = "status", required = false) List<String> statusList,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        log.info("【MokaEntryCandidateController】收到 Step 2 推送 iHR 请求, statusList={}, limit={}", statusList, limit);
        MokaEntryPushSummary summary = pushManager.pushToIhr(statusList, limit);
        return Result.success(summary);
    }
}
