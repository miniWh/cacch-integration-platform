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
     * Moka 待入职候选人--Moka&PG
     */
    @PostMapping("/sync")
    public Result<MokaEntrySyncResult> sync() {
        log.info("【MokaEntryCandidateController】收到第一步同步请求");
        MokaEntrySyncResult result = syncManager.syncEntryCandidates();
        return Result.success(result);
    }

    /**
     * Moka 待入职候选人--PG&iHR
     */
    @PostMapping("/push-ihr")
    public Result<String> pushToIhr() {
        log.info("【MokaEntryCandidateController】收到第二步推送 iHR 请求，当前版本尚未实现");
        return Result.fail(com.cacch.integration.common.result.ResultCode.PARAM_ERROR,
                "第二步推送 iHR 待入职尚未实现，请先调用 /sync 完成第一步");
    }
}
