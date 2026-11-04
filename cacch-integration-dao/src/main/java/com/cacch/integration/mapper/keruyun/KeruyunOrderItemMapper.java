package com.cacch.integration.mapper.keruyun;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cacch.integration.entity.keruyun.KeruyunOrderItemDO;
import com.cacch.integration.query.keruyun.KeruyunOrderItemQueryCondition;
import com.cacch.integration.query.keruyun.KeruyunOrderItemQueryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 客如云订单菜品明细落库 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface KeruyunOrderItemMapper extends BaseMapper<KeruyunOrderItemDO> {

    /**
     * 菜品明细查询 SQL 公共片段（明细表 JOIN 订单主表）
     *
     * <p>订单状态/类型 CASE WHEN 转中文（未识别的枚举返回原始值）；
     * 金额字段 ROUND(x/100.0, 2) 分转元；
     * 排序：下单时间倒序、同订单 SKU 相邻（按订单 ID、行 ID 排列）。
     * 片段不闭合 &lt;/script&gt; 标签，由各查询方法按需拼接 LIMIT 后闭合。</p>
     */
    String ITEM_QUERY_SQL = """
            <script>
            SELECT i.order_id                        AS order_id,
                   i.item_code                       AS item_code,
                   i.item_name                       AS item_name,
                   i.big_type_name                   AS big_type_name,
                   i.mid_type_name                   AS mid_type_name,
                   i.unit_name                       AS unit_name,
                   i.spec_name_concat                AS spec_name,
                   i.quantity                        AS quantity,
                   i.gift_flag                       AS gift_flag,
                   i.sale_status_type_code           AS sale_status_type_code,
                   ROUND(i.item_price / 100.0, 2)    AS item_price_yuan,
                   ROUND(i.sale_price / 100.0, 2)    AS sale_price_yuan,
                   ROUND(i.item_sale_amt / 100.0, 2) AS item_sale_amt_yuan,
                   o.shop_id                        AS shop_id,
                   o.busi_order_no                  AS busi_order_no,
                   CASE o.order_status
                        WHEN 'WAIT_PROCESSED' THEN '待处理'
                        WHEN 'ORDER_RECEIVED' THEN '已接单'
                        WHEN 'SUCCESS'        THEN '成功'
                        WHEN 'WAIT_SETTLED'   THEN '待结账'
                        WHEN 'SETTLED'        THEN '已结账'
                        WHEN 'REFUND'         THEN '已退单'
                        WHEN 'CLOSED'         THEN '已关闭'
                        WHEN 'INVALID'        THEN '已作废'
                        WHEN 'CANCELLED'      THEN '已取消'
                        WHEN 'REJECTED'       THEN '已拒单'
                        WHEN 'ANTI_SETTLED'   THEN '已反结'
                        ELSE o.order_status
                   END                              AS order_status,
                   CASE o.order_type
                        WHEN 'FOR_HERE'            THEN '堂食'
                        WHEN 'PLATFORM_TAKE_OUT'   THEN '平台外卖'
                        WHEN 'SELF_TAKE_OUT'       THEN '门店外带'
                        WHEN 'SELF_TAKE'           THEN '自提'
                        WHEN 'NO_ORDER_CASHIER'    THEN '无单收银'
                        WHEN 'MEMBER_STORE'        THEN '会员储值'
                        WHEN 'MEMBER_MANUAL_STORE' THEN '会员手工储值'
                        WHEN 'REPAYMENT_ORDER'     THEN '销账订单'
                        ELSE o.order_type
                   END                              AS order_type,
                   o.open_time                      AS open_time,
                   o.finish_time                    AS finish_time
            FROM stg_keruyun_order_item i
            JOIN stg_keruyun_order o ON o.order_id = i.order_id AND o.is_deleted = 0
            WHERE i.is_deleted = 0
            <if test="cond.orderId != null and cond.orderId != ''">
                AND i.order_id = #{cond.orderId}
            </if>
            <if test="cond.itemCode != null and cond.itemCode != ''">
                AND i.item_code = #{cond.itemCode}
            </if>
            <if test="cond.itemName != null and cond.itemName != ''">
                AND i.item_name LIKE CONCAT('%', #{cond.itemName}, '%')
            </if>
            <if test="cond.orderStatus != null and cond.orderStatus != ''">
                AND o.order_status = #{cond.orderStatus}
            </if>
            <if test="cond.orderType != null and cond.orderType != ''">
                AND o.order_type = #{cond.orderType}
            </if>
            <if test="cond.openTimeFrom != null">
                AND o.open_time &gt;= #{cond.openTimeFrom}
            </if>
            <if test="cond.openTimeTo != null">
                AND o.open_time &lt;= #{cond.openTimeTo}
            </if>
            ORDER BY o.open_time DESC, i.order_id, i.item_row_id
            """;

    /**
     * 分页查询订单菜品明细（SKU 维度，一行 = 一个订单 × 一个 SKU）
     *
     * <p>分页由 MyBatis-Plus 分页拦截器追加 LIMIT 实现；
     * 订单状态/类型过滤使用客如云原始枚举值，展示列已转换为中文。</p>
     *
     * @param page 分页参数（页码、每页条数），不可为 null
     * @param cond 查询条件（所有条件可选）
     * @return 分页结果，records 为明细查询行；无数据时 records 为空列表（非 null）
     */
    @Select(ITEM_QUERY_SQL + "</script>")
    IPage<KeruyunOrderItemQueryRow> pageQueryItems(Page<KeruyunOrderItemQueryRow> page,
                                                   @Param("cond") KeruyunOrderItemQueryCondition cond);

    /**
     * 全量查询订单菜品明细（导出专用，不分页）
     *
     * <p>按下单时间倒序返回；调用方传入 limit = maxRows + 1 用于检测是否超限。</p>
     *
     * @param cond  查询条件（所有条件可选）
     * @param limit 最大返回条数（含超限检测的额外 1 行），必须大于 0
     * @return 明细查询行列表；无数据时返回空列表（非 null）
     */
    @Select(ITEM_QUERY_SQL + " LIMIT #{limit} </script>")
    List<KeruyunOrderItemQueryRow> listItemsForExport(@Param("cond") KeruyunOrderItemQueryCondition cond,
                                                      @Param("limit") int limit);
}
