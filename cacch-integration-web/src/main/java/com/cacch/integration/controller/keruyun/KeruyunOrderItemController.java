package com.cacch.integration.controller.keruyun;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.keruyun.request.KeruyunOrderItemQueryRequest;
import com.cacch.integration.query.keruyun.KeruyunOrderItemQueryRow;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客如云订单菜品明细 REST 接口（SKU 维度）
 *
 * <p>一行 = 一个订单 × 一个 SKU，用于以菜品为维度的订单信息展示。
 * 数据来源：stg_keruyun_order_item JOIN stg_keruyun_order，
 * 依赖订单详情同步（列表同步完成后自动触发）落库。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@RestController
@RequestMapping("/api/v1/keruyun/order-items")
@RequiredArgsConstructor
public class KeruyunOrderItemController {

    private final IKeruyunOrderDetailService keruyunOrderDetailService;

    /**
     * 按条件分页查询订单菜品明细（SKU 维度）
     *
     * <p>所有过滤条件均为可选；未传条件时按下单时间倒序返回，同一订单的 SKU 行相邻排列。
     * 返回行中订单状态/类型已转换为中文，金额已从分转换为元（保留 2 位小数）。</p>
     *
     * @param request 查询参数（所有条件可选，pageNum 默认 1、pageSize 默认 20 最大 100）
     * @return 分页结果，records 为明细行（订单头信息 + SKU 数量/单价/金额）；无数据时 records 为空列表（非 null）
     */
    @GetMapping
    public Result<IPage<KeruyunOrderItemQueryRow>> query(@Valid KeruyunOrderItemQueryRequest request) {
        IPage<KeruyunOrderItemQueryRow> page = keruyunOrderDetailService.pageQueryItems(
                request.getOrderId(),
                request.getItemCode(),
                request.getItemName(),
                request.getOrderStatus(),
                request.getOrderType(),
                request.getOpenTimeFrom(),
                request.getOpenTimeTo(),
                request.getPageNum(),
                request.getPageSize());
        return Result.success(page);
    }

    /**
     * 按条件全量查询订单菜品明细（Excel 导出专用，不分页）
     *
     * <p>供管理端导出 Excel 时直接调用，返回满足条件的全部明细行。
     * 实际返回行数超过默认上限 50000 条时返回 400 业务码，提示调用方缩小查询范围。
     * 排序与分页接口一致（下单时间倒序、同订单 SKU 相邻）。</p>
     *
     * @param request 查询参数（所有条件可选；不含分页参数）
     * @return 明细行列表；无数据时返回空列表（非 null）
     */
    @GetMapping("/export")
    public Result<List<KeruyunOrderItemQueryRow>> export(@Valid KeruyunOrderItemQueryRequest request) {
        // 默认最大返回 50000 条，超过提示缩小范围
        final int defaultMaxRows = 50000;
        List<KeruyunOrderItemQueryRow> rows = keruyunOrderDetailService.listItemsForExport(
                request.getOrderId(),
                request.getItemCode(),
                request.getItemName(),
                request.getOrderStatus(),
                request.getOrderType(),
                request.getOpenTimeFrom(),
                request.getOpenTimeTo(),
                defaultMaxRows);
        return Result.success(rows);
    }
}
