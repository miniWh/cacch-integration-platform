package com.cacch.integration.query.keruyun;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客如云订单菜品明细查询条件
 *
 * <p>所有条件均为可选；itemName 为模糊匹配，其余均为精确匹配。
 * orderStatus / orderType 使用客如云原始枚举值过滤（查询结果中已转换为中文展示值）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderItemQueryCondition {

    /**
     * 客如云订单 ID（精确匹配）
     */
    private String orderId;

    /**
     * SKU 编码（精确匹配）
     */
    private String itemCode;

    /**
     * 菜品名称（模糊匹配）
     */
    private String itemName;

    /**
     * 订单状态（客如云原始枚举值，精确匹配）
     */
    private String orderStatus;

    /**
     * 订单类型（客如云原始枚举值，精确匹配）
     */
    private String orderType;

    /**
     * 下单时间起始（含）
     */
    private LocalDateTime openTimeFrom;

    /**
     * 下单时间结束（含）
     */
    private LocalDateTime openTimeTo;
}
