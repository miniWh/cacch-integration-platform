package com.cacch.integration.service.keruyun.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderItemDO;
import com.cacch.integration.entity.keruyun.KeruyunOrderPaymentDO;
import com.cacch.integration.query.keruyun.KeruyunOrderItemQueryRow;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客如云订单详情持久化服务接口
 *
 * <p>职责：待同步完结订单查询（主表）、单订单菜品/支付明细的幂等落库。
 * 落库策略为每单整体 delete + insert，与主表 detail_synced_at 更新在同一事务内完成。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunOrderDetailService {

    /**
     * 查询待同步详情的完结订单（finish_time 非空且 detail_synced_at 为空）
     *
     * <p>按完结时间升序返回，最多 limit 条。</p>
     *
     * @param limit 最大返回条数，必须大于 0
     * @return 待同步订单列表；无待同步数据时返回空列表（非 null）
     */
    List<KeruyunOrderDO> listOrdersPendingDetail(int limit);

    /**
     * 幂等落库单订单的菜品与支付明细
     *
     * <p>整体替换策略：先删除该订单已有的菜品/支付明细，再插入新数据，
     * 最后更新主表 detail_synced_at；三步在同一事务内完成，失败整体回滚。
     * 明细列表均可为空（如整单退款无支付明细），此时仅清空旧数据并标记已同步。</p>
     *
     * @param orderId  客如云订单 ID，不可为空
     * @param items    菜品明细列表（可为空列表）
     * @param payments 支付明细列表（可为空列表）
     */
    void saveOrderDetails(String orderId, List<KeruyunOrderItemDO> items, List<KeruyunOrderPaymentDO> payments);

    /**
     * 分页查询订单菜品明细（SKU 维度，含订单头信息）
     *
     * <p>一行 = 一个订单 × 一个 SKU，按下单时间倒序、同一订单的 SKU 相邻排列。
     * 订单状态/类型已在 SQL 中转换为中文，金额已从分转换为元（保留 2 位小数）。
     * 简单只读查询，不开事务。</p>
     *
     * @param orderId      客如云订单 ID，精确匹配，可空
     * @param itemCode     SKU 编码，精确匹配，可空
     * @param itemName     菜品名称，模糊匹配，可空
     * @param orderStatus  订单状态（客如云原始枚举值），精确匹配，可空
     * @param orderType    订单类型（客如云原始枚举值），精确匹配，可空
     * @param openTimeFrom 下单时间起始（含），可空
     * @param openTimeTo   下单时间结束（含），可空
     * @param pageNum      页码，从 1 开始，小于 1 时按 1 处理
     * @param pageSize     每页条数，1~100，超出自动截断为 100
     * @return 分页结果，records 为明细查询行；无数据时 records 为空列表（非 null）
     */
    IPage<KeruyunOrderItemQueryRow> pageQueryItems(String orderId, String itemCode, String itemName,
                                                   String orderStatus, String orderType,
                                                   LocalDateTime openTimeFrom, LocalDateTime openTimeTo,
                                                   int pageNum, int pageSize);

    /**
     * 全量查询订单菜品明细（Excel 导出专用，不分页）
     *
     * <p>按下单时间倒序返回满足条件的全部明细行；实际行数超过 maxRows 时抛出业务异常，
     * 提示调用方缩小查询范围。</p>
     *
     * @param orderId      客如云订单 ID，精确匹配，可空
     * @param itemCode     SKU 编码，精确匹配，可空
     * @param itemName     菜品名称，模糊匹配，可空
     * @param orderStatus  订单状态（客如云原始枚举值），精确匹配，可空
     * @param orderType    订单类型（客如云原始枚举值），精确匹配，可空
     * @param openTimeFrom 下单时间起始（含），可空
     * @param openTimeTo   下单时间结束（含），可空
     * @param maxRows      最大返回条数，必须大于 0
     * @return 明细查询行列表；无数据时返回空列表（非 null）
     * @throws com.cacch.integration.common.exception.BizException 结果超过 maxRows 时抛出
     */
    List<KeruyunOrderItemQueryRow> listItemsForExport(String orderId, String itemCode, String itemName,
                                                      String orderStatus, String orderType,
                                                      LocalDateTime openTimeFrom, LocalDateTime openTimeTo,
                                                      int maxRows);
}
