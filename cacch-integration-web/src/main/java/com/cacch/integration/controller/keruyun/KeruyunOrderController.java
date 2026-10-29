package com.cacch.integration.controller.keruyun;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncBatchResult;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.convert.keruyun.KeruyunOrderConverter;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderQueryRequest;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderSyncRequest;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderSyncTaskExecuteRequest;
import com.cacch.integration.dto.keruyun.vo.KeruyunOrderVO;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderManager;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderSyncTaskManager;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客如云订单接口
 *
 * <p>提供三类入口：本地查询（已落库数据）、手动透传同步（调试）、
 * 控制表驱动的任务化同步（幂等 + 断点续传）。</p>
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
    private final IKeruyunOrderService orderService;
    private final KeruyunOrderConverter orderConverter;

    /**
     * 手动指定时间窗口同步（调试 / 快速验证入口）
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
     * @param request 批次触发参数（anchorDate 不传默认昨天，dateType 不传默认 OPEN_TIME）
     * @return 批次执行结果（新建数量、待执行数量、任务 ID 列表）
     */
    @PostMapping("/sync/task")
    public Result<KeruyunOrderSyncBatchResult> executeBatch(@RequestBody KeruyunOrderSyncTaskExecuteRequest request) {
        KeruyunOrderSyncBatchResult result = taskManager.executeBatch(
                request.getAnchorDate(),
                request.getDateType());
        return Result.success(result);
    }

    /**
     * 按客如云订单 ID 查询单条（本地已落库数据）
     *
     * @param orderId 客如云订单 ID，业务唯一键
     * @return 订单视图对象；不存在时由全局异常处理返回 404 业务码
     */
    @GetMapping("/{orderId}")
    public Result<KeruyunOrderVO> getByOrderId(@PathVariable String orderId) {
        KeruyunOrderDO order = orderService.getByOrderId(orderId);
        if (order == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "客如云订单不存在: " + orderId);
        }
        return Result.success(orderConverter.toVO(order));
    }

    /**
     * 按条件分页查询本地已落库订单
     *
     * <p>所有过滤条件均为可选；未传条件时按 open_time 倒序返回。
     * 金额字段返回已从分转换为元（保留 2 位小数）。</p>
     *
     * @param request 查询参数（所有条件可选，pageNum 默认 1、pageSize 默认 20 最大 100）
     * @return 分页结果，records 为 VO 列表；无数据时 records 为空列表（非 null）
     */
    @GetMapping
    public Result<IPage<KeruyunOrderVO>> query(@Valid KeruyunOrderQueryRequest request) {
        IPage<KeruyunOrderDO> page = orderService.queryByCondition(
                request.getOrderId(),
                request.getShopId(),
                request.getOrderStatus(),
                request.getOrderType(),
                request.getOpenTimeFrom(),
                request.getOpenTimeTo(),
                request.getCreatedAtFrom(),
                request.getCreatedAtTo(),
                request.getPageNum(),
                request.getPageSize());
        // DO → VO（金额分转元在 Converter 内完成）
        IPage<KeruyunOrderVO> voPage = page.convert(orderConverter::toVO);
        return Result.success(voPage);
    }
}
