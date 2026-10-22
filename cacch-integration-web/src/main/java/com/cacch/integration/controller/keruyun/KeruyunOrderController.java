package com.cacch.integration.controller.keruyun;

import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderSyncRequest;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客如云订单接口
 *
 * @author hongfu_zhou@cacch.com
 */
@RestController
@RequestMapping("/api/v1/keruyun/orders")
@RequiredArgsConstructor
public class KeruyunOrderController {

    private final IKeruyunOrderManager keruyunOrderManager;

    /**
     * 订单列表--Keruyun&PG
     *
     * <p>客如云文档明确禁止轮询，本接口仅供一次性按区间拉取；单条落库失败不影响整批。</p>
     *
     * @param request 时间窗、订单类型/状态过滤与分页大小
     * @return 同步统计结果（页数、扫描/新增/更新/跳过/失败条数）
     */
    @PostMapping("/sync")
    public Result<KeruyunOrderSyncResult> syncOrders(@Valid @RequestBody KeruyunOrderSyncRequest request) {
        return Result.success(keruyunOrderManager.syncOrders(
                request.getDateType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getOrderTypeList(),
                request.getOrderStatusList(),
                request.getPageSize()));
    }
}
