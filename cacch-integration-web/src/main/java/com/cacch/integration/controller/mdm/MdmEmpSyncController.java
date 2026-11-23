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
     * ESB 增量同步单接口 —— 编排 Pull 入队后 Process 落库
     *
     * @param limit Process 单次认领上限，默认 500，有效范围 1～1000
     * @return {@code pullStatus}/{@code processStatus} 及分段统计
     */
    @PostMapping("/incremental-sync")
    public Result<IMdmEmpSyncManager.IncrementalSyncResult> incrementalSync(@RequestParam(defaultValue = "500") int limit) {
        log.info("【{}】ESB 触发增量编排（Pull+Process）, limit={}", BIZ, limit);
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        return Result.success(mdmEmpSyncManager.runIncrementalSync(safeLimit));
    }
}
