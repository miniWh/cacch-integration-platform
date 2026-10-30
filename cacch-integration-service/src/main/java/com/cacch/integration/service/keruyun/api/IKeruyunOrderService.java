package com.cacch.integration.service.keruyun.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客如云订单持久化服务接口
 *
 * <p>单聚合（订单快照）内的业务逻辑：以 order_id 为业务唯一键执行幂等写入，
 * 以及按条件分页查询 / 全量查询本地已落库订单。不做跨聚合编排与第三方调用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunOrderService {

    /**
     * 按 order_id 幂等写入订单（存在则更新，不存在则新增）
     *
     * @param orderDO 待写入订单，orderId 不可为空；新增时忽略传入的 id
     * @return true 表示新增，false 表示更新
     * @throws com.cacch.integration.common.exception.BizException orderId 为空时抛出
     */
    boolean saveOrUpdate(KeruyunOrderDO orderDO);

    /**
     * 按客如云订单 ID 精确查询单条
     *
     * @param orderId 客如云订单 ID，业务唯一键
     * @return 订单 DO；不存在时返回 null
     */
    KeruyunOrderDO getByOrderId(String orderId);

    /**
     * 按条件分页查询本地已落库订单
     *
     * <p>所有过滤条件均为可选；未传条件时按 open_time 倒序返回。</p>
     *
     * @param orderId       客如云订单 ID（精确匹配，可 null）
     * @param shopId        门店 ID（精确匹配，可 null）
     * @param orderStatus   订单状态（精确匹配，可 null）
     * @param orderType     订单类型（精确匹配，可 null）
     * @param openTimeFrom  下单时间起始（含，可 null）
     * @param openTimeTo    下单时间结束（含，可 null）
     * @param createdAtFrom 记录创建时间起始（含，可 null）
     * @param createdAtTo   记录创建时间结束（含，可 null）
     * @param pageNum       页码，从 1 开始
     * @param pageSize      每页条数，最大 100（超出截断）
     * @return 分页结果；无数据时 records 为空列表（非 null）
     */
    IPage<KeruyunOrderDO> queryByCondition(String orderId, String shopId, String orderStatus, String orderType,
                                           LocalDateTime openTimeFrom, LocalDateTime openTimeTo,
                                           LocalDateTime createdAtFrom, LocalDateTime createdAtTo,
                                           int pageNum, int pageSize);

    /**
     * 按条件全量查询订单（Excel 导出专用，不分页）
     *
     * <p>与 {@link #queryByCondition} 使用相同的过滤条件，但返回满足条件的全部数据。
     * 为避免内存风险，实际返回行数超过 {@code maxRows} 时抛出业务异常，提示调用方缩小查询范围。
     * 排序按 open_time 升序（导出场景一般按时间正序）。</p>
     *
     * @param orderId       客如云订单 ID（精确匹配，可 null）
     * @param shopId        门店 ID（精确匹配，可 null）
     * @param orderStatus   订单状态（精确匹配，可 null）
     * @param orderType     订单类型（精确匹配，可 null）
     * @param openTimeFrom  下单时间起始（含，可 null）
     * @param openTimeTo    下单时间结束（含，可 null）
     * @param createdAtFrom 记录创建时间起始（含，可 null）
     * @param createdAtTo   记录创建时间结束（含，可 null）
     * @param maxRows       最大返回行数，超过即抛异常；必须大于 0
     * @return 订单 DO 列表；无数据时返回空列表（非 null）
     * @throws com.cacch.integration.common.exception.BizException 实际结果超出 maxRows 时抛出
     */
    List<KeruyunOrderDO> listForExport(String orderId, String shopId, String orderStatus, String orderType,
                                       LocalDateTime openTimeFrom, LocalDateTime openTimeTo,
                                       LocalDateTime createdAtFrom, LocalDateTime createdAtTo,
                                       int maxRows);
}
