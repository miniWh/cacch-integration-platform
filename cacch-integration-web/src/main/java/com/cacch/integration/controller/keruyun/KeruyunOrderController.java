package com.cacch.integration.controller.keruyun;

import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncBatchResult;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderSyncRequest;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderSyncTaskExecuteRequest;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderManager;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderSyncTaskManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客如云订单接口
 *
 * <p>提供两类同步入口：手动透传查询（一次性按区间拉取）、
 * 控制表驱动的任务化同步（检查缺失 → 新建 → 逐条执行 → 断点续传）。</p>
 *
 * <p>客如云文档明确禁止轮询，控制表驱动方式已内置幂等与断点续传，
 * 避免对客如云接口的重复调用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@RestController
@RequestMapping("/api/v1/keruyun/orders")
@RequiredArgsConstructor
public class KeruyunOrderController {

    private final IKeruyunOrderManager keruyunOrderManager;
    private final IKeruyunOrderSyncTaskManager taskManager;

    /**
     * 手动指定时间窗口同步（调试/快速验证入口）
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

    /**
     * 触发批次同步（两步编排）
     *
     * <p>① 确保基准日期的控制表记录存在（不存在则新建 PENDING）；
     * ② 取所有 PENDING 任务逐条异步执行；立即返回 BatchResult，不等待执行完成。</p>
     *
     * @param request 批次触发参数（anchorDate 不传默认昨天，dateType 不传对全部执行）
     * @return 批次执行结果（新建数量、待执行数量、任务 ID 列表）
     */
    @PostMapping("/sync/task")
    public Result<KeruyunOrderSyncBatchResult> executeBatch(@RequestBody KeruyunOrderSyncTaskExecuteRequest request) {
        KeruyunOrderSyncBatchResult result = taskManager.executeBatch(
                request.getAnchorDate(),
                request.getDateType());
        return Result.success(result);
    }
}
