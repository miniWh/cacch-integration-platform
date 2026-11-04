package com.cacch.integration.controller.keruyun;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderDetailSyncResult;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncBatchResult;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.convert.keruyun.KeruyunOrderSyncTaskConverter;
import com.cacch.integration.convert.keruyun.KeruyunOrderVOConverter;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderDetailSyncRequest;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderManualSyncRequest;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderQueryRequest;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderSyncRequest;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderSyncTaskExecuteRequest;
import com.cacch.integration.dto.keruyun.vo.KeruyunOrderSyncTaskVO;
import com.cacch.integration.dto.keruyun.vo.KeruyunOrderVO;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderSyncTaskDO;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderDetailManager;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderManager;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderSyncTaskManager;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderService;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderSyncTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final IKeruyunOrderDetailManager orderDetailManager;
    private final IKeruyunOrderSyncTaskManager taskManager;
    private final IKeruyunOrderService orderService;
    private final IKeruyunOrderSyncTaskService taskService;
    private final KeruyunOrderVOConverter orderConverter;
    private final KeruyunOrderSyncTaskConverter taskConverter;

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
     * 手动触发跨天同步（单任务，dateType 固定 OPEN_TIME）
     *
     * <p>调用方仅需指定查询窗口起始与结束时间，后端固定 dateType=OPEN_TIME、
     * 以 startTime 的日期作为 sync_date 创建（或重置）一条任务并异步执行。
     * 立即返回 BatchResult，不等待执行完成。</p>
     *
     * @param request 手动同步请求（startTime / endTime 必填，endTime 必须晚于 startTime）
     * @return 批次执行结果（含任务 ID）
     */
    @PostMapping("/sync/tasks/manual")
    public Result<KeruyunOrderSyncBatchResult> executeManualBatch(
            @Valid @RequestBody KeruyunOrderManualSyncRequest request) {
        KeruyunOrderSyncBatchResult result = taskManager.executeManualBatch(
                request.getStartTime(),
                request.getEndTime());
        return Result.success(result);
    }

    /**
     * 分页查询同步任务记录
     *
     * <p>从 stg_keruyun_order_sync_task 表读取，按 sync_date 倒序排列。
     * 供管理端「客如云订单同步」页面展示同步记录。</p>
     *
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 20，最大 100
     * @return 分页结果，records 为任务 VO 列表
     */
    @GetMapping("/sync/tasks")
    public Result<IPage<KeruyunOrderSyncTaskVO>> querySyncTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<KeruyunOrderSyncTaskDO> page = taskService.pageQuery(pageNum, pageSize);
        IPage<KeruyunOrderSyncTaskVO> voPage = page.convert(taskConverter::toVO);
        return Result.success(voPage);
    }

    /**
     * 批量同步完结订单详情（菜品/支付明细，固定并发）
     *
     * <p>仅处理已完结（finish_time 非空）且详情未同步（detail_synced_at 为空）的订单；
     * 固定 5 并发拉取客如云详情接口（限流 220 QPS），单订单失败不阻断整批，
     * 失败订单下次触发时自动重试。同步执行，处理上限默认 200 单，剩余可重复触发。</p>
     *
     * @param request 批量参数（limit 可选：单次处理上限，默认 200、最大 1000）
     * @return 同步统计结果（成功/失败/跳过条数与明细、落库菜品/支付条数）
     */
    @PostMapping("/detail/sync")
    public Result<KeruyunOrderDetailSyncResult> syncOrderDetails(@RequestBody(required = false) KeruyunOrderDetailSyncRequest request) {
        Integer limit = request == null ? null : request.getLimit();
        return Result.success(orderDetailManager.syncOrderDetails(limit));
    }

    /**
     * 同步单条订单详情（调试 / 单单补偿入口）
     *
     * <p>显式指定订单即视为重新拉取：已同步过的订单也会覆盖落库；
     * 订单不存在或未完结时返回对应业务码。</p>
     *
     * @param orderId 客如云订单 ID，业务唯一键
     * @return 同步统计结果（成功/跳过明细）
     */
    @PostMapping("/detail/sync/{orderId}")
    public Result<KeruyunOrderDetailSyncResult> syncOrderDetail(@PathVariable String orderId) {
        return Result.success(orderDetailManager.syncOrderDetail(orderId));
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

    /**
     * 按条件全量查询订单（Excel 导出专用，不分页）
     *
     * <p>供管理端导出 Excel 时直接调用，返回满足条件的全部订单数据。
     * 实际返回行数超过默认上限 50000 条时返回 400 业务码，提示调用方缩小查询范围。
     * 金额字段返回已从分转换为元（保留 2 位小数），按 open_time 升序排列。</p>
     *
     * @param request 查询参数（所有条件可选；不含分页参数）
     * @return 订单 VO 列表；无数据时返回空列表（非 null）
     */
    @GetMapping("/export")
    public Result<List<KeruyunOrderVO>> export(@Valid KeruyunOrderQueryRequest request) {
        // 默认最大返回 50000 条，超过提示缩小范围
        final int defaultMaxRows = 50000;
        List<KeruyunOrderDO> rows = orderService.listForExport(
                request.getOrderId(),
                request.getShopId(),
                request.getOrderStatus(),
                request.getOrderType(),
                request.getOpenTimeFrom(),
                request.getOpenTimeTo(),
                request.getCreatedAtFrom(),
                request.getCreatedAtTo(),
                defaultMaxRows);
        List<KeruyunOrderVO> voList = rows.stream()
                .map(orderConverter::toVO)
                .toList();
        return Result.success(voList);
    }
}
