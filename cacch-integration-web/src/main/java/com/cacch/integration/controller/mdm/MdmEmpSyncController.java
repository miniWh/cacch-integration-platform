package com.cacch.integration.controller.mdm;

import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
        log.info("【{}】ESB 触发人员同步", BIZ);
        return Result.success(mdmEmpSyncManager.pullAndIngest());
    }
}
