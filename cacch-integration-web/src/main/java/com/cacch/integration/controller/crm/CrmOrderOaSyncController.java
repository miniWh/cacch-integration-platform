package com.cacch.integration.controller.crm;

import com.cacch.integration.common.dto.crm.CrmOrderOaSyncResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.manager.crm.api.ICrmOrderOaSyncManager;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 订单 OA 同步联调接口
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/crm/sync")
@RequiredArgsConstructor
public class CrmOrderOaSyncController {

    private final ICrmOrderOaSyncManager crmOrderOaSyncManager;

    /**
     * 手动触发 OA 表单同步（扫描 PENDING/RETRY 明细）
     *
     * @return 本轮同步统计
     */
    @PostMapping("/orders")
    public Result<CrmOrderOaSyncResult> syncOrders() {
        return Result.success(crmOrderOaSyncManager.syncPendingDetails());
    }
}
