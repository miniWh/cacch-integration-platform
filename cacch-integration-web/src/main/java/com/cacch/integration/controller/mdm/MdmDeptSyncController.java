package com.cacch.integration.controller.mdm;

import com.cacch.integration.common.constant.mdm.MdmDeptLogConstants;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.manager.mdm.api.IMdmDeptSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门主数据（MDM）iHR 部门同步 REST 接口
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mdm/dept-sync")
@RequiredArgsConstructor
public class MdmDeptSyncController {

    private static final String BIZ = MdmDeptLogConstants.LOG_BIZ;

    private final IMdmDeptSyncManager mdmDeptSyncManager;

    /**
     * 触发一轮 iHR 部门全量同步（拉取 → 贴源 → 规范化 → 主档 → 事件 → 停用比对 → 核对）
     *
     * <p>内部互斥：上一轮仍在执行中时拒绝触发（返回业务失败码）；
     * 停用比对在拉取为空或存在失败时自动跳过（防误停用）。</p>
     *
     * @return 本轮同步结果（totalCount / successCount / skipCount / failureCount /
     * disabledCount / 一致性核对明细）
     */
    @PostMapping("/trigger")
    public Result<IMdmDeptSyncManager.DeptSyncResult> trigger() {
        log.info("【{}】ESB 触发全量部门同步", BIZ);
        return Result.success(mdmDeptSyncManager.fullSync());
    }
}
