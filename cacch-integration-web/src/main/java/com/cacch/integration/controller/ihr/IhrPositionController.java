package com.cacch.integration.controller.ihr;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.manager.ihr.api.IIhrPositionSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IHR 职位快照 REST 接口
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/ihr/positions")
@RequiredArgsConstructor
public class IhrPositionController {

    private final IIhrPositionSyncManager ihrPositionSyncManager;

    /**
     * 手动触发 IHR 职位全量同步 — 拉取全量职位清单并 upsert 到本地快照表 {@code t_integration_ihr_position}
     *
     * <p>同步过程：一次拉取 iHR 全量职位（接口无分页）→ ON CONFLICT(uuid) DO UPDATE 分批落库。
     * 同步批次号（sync_batch）自动生成 UUID，便于事后追溯。</p>
     *
     * <p>注意：当前实现未做请求去重或分布式锁（单机串行调用即可；并发触发时最后一次成功的批次将全部覆盖，
     * 但不会破坏数据完整性——ON CONFLICT 保证幂等）。如需定时同步可后续追加 @Scheduled 调用。</p>
     *
     * @return 同步结果摘要（totalFetched / upserted / skipped）
     */
    @PostMapping("/sync")
    public Result<IIhrPositionSyncManager.PositionSyncResult> syncAll() {
        log.info("收到职位全量同步请求");
        return Result.success(ihrPositionSyncManager.syncAll());
    }
}
