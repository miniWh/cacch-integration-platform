package com.cacch.integration.controller.moka;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.manager.moka.api.IMokaEntryCandidateSyncManager;
import com.cacch.integration.manager.moka.api.IMokaEntryCandidateSyncManager.MokaEntrySyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moka 待入职候选人同步 REST 接口
 *
 * <p>本 Controller 承载两步接口：
 * <ol>
 *     <li>第一步（本次实现）：Moka → 本地快照 + 补全
 *         POST /api/v1/moka/entry-candidates/sync</li>
 *     <li>第二步（TODO）：本地快照 → iHR 待入职
 *         POST /api/v1/moka/entry-candidates/push-ihr</li>
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

    /**
     * 第一步：Moka 待入职候选人 → 本地快照 + 补全
     *
     * <p>无入参。内部执行三阶段：
     * <ol>
     *     <li>Step 1a：分页拉取 Moka list_by_condition（pipelineIds=[163155,163156], stageIds=[8]）
     *         → 4 表幂等 upsert（主表 + candidate_base + graduate_school + citizen + 状态表）</li>
     *     <li>Step 1b：按 jobId 去重查职位详情 → 回写 moka_department_code</li>
     *     <li>Step 1c：按 applicationId 批量查 Offer 扩展 → 回写 check_in_date / offer_department_code</li>
     * </ol></p>
     *
     * @return 同步统计（1a 入库数 / 1b 部门补全数 / 1c Offer 补全数）
     */
    @PostMapping("/sync")
    public Result<MokaEntrySyncResult> sync() {
        log.info("【MokaEntryCandidateController】收到第一步同步请求");
        MokaEntrySyncResult result = syncManager.syncEntryCandidates();
        return Result.success(result);
    }

    /**
     * 第二步：本地快照 → iHR 待入职（TODO）
     *
     * <p>从本地快照表（moka_entry_application 等）读取 ihr_sync_status='PENDING' 的记录，
     * 匹配 iHR 部门，调用 iHR 新增待入职接口，更新同步状态。
     * 本接口将在第二步开发时实现。</p>
     *
     * @return 同步统计
     */
    @PostMapping("/push-ihr")
    public Result<String> pushToIhr() {
        log.info("【MokaEntryCandidateController】收到第二步推送 iHR 请求，当前版本尚未实现");
        return Result.fail(com.cacch.integration.common.result.ResultCode.PARAM_ERROR,
                "第二步推送 iHR 待入职尚未实现，请先调用 /sync 完成第一步");
    }
}
