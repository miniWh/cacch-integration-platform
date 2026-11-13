package com.cacch.integration.controller.mdm;

import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人员主数据（MDM）iHR 人员同步 REST 接口 —— 供 ESB 定时调度调用
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mdm/emp-sync")
@RequiredArgsConstructor
public class MdmEmpSyncController {

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;

    private final IMdmEmpSyncManager mdmEmpSyncManager;

    /**
     * 触发一轮 iHR 人员全量拉取落库（拉取 → 贴源 → 规范化 → 主档 → 任职 → 事件）
     *
     * @return 本轮批量处理统计（totalCount / successCount / skipCount / failureCount / details）
     */
    @PostMapping("/trigger")
    public Result<BatchProcessResult<String>> trigger() {
        log.info("【{}】ESB 触发全量人员同步", BIZ);
        return Result.success(mdmEmpSyncManager.pullAndIngest());
    }

    /**
     * 触发一轮增量 Pull —— 从 iHR 拉取新增 + 更新员工 ID 入队 stg_staff_record
     *
     * <p>ESB 建议每 5 分钟调用一次。内部自动并发 6 次 HTTP（3 类接口 × T + T-1 双窗口），
     * 入队幂等：UNIQUE(staff_id, pull_batch_no) DO UPDATE。</p>
     *
     * @return 本轮入队统计（按 source 分组）
     */
    @PostMapping("/pull-incremental")
    public Result<IMdmEmpSyncManager.PullIncrementalResult> pullIncremental() {
        log.info("【{}】ESB 触发增量 Pull 入队", BIZ);
        return Result.success(mdmEmpSyncManager.pullIncrementalToQueue());
    }

    /**
     * 触发一轮增量 Process —— 从 stg_staff_record 捞取 PENDING 行逐人落库
     *
     * <p>ESB 建议每 2 分钟调用一次。捞取规则：</p>
     * <ul>
     *     <li>FOR UPDATE SKIP LOCKED 防多实例并发</li>
     *     <li>ORDER BY source='ADD' 先于 'UPDATE'</li>
     *     <li>UPDATE 内部按 last_update 秒级时间戳升序</li>
     * </ul>
     *
     * @param limit 单次处理上限，默认 500
     * @return 本轮批量处理统计
     */
    @PostMapping("/process-incremental")
    public Result<BatchProcessResult<String>> processIncremental(@RequestParam(defaultValue = "500") int limit) {
        log.info("【{}】ESB 触发增量 Process 落库, limit={}", BIZ, limit);
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        return Result.success(mdmEmpSyncManager.processIncrementalFromQueue(safeLimit));
    }
}
