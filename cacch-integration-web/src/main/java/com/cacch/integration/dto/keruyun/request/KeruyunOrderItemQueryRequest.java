package com.cacch.integration.dto.keruyun.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客如云订单菜品明细查询请求参数
 *
 * <p>所有过滤条件均为可选；未传条件时按下单时间倒序返回，
 * 同一订单的 SKU 行相邻排列。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderItemQueryRequest {

    /**
     * 客如云订单 ID（精确匹配）
     */
    private String orderId;

    /**
     * SKU 编码，对应 stg_keruyun_order_item.item_code（精确匹配）
     */
    private String itemCode;

    /**
     * 菜品名称（模糊匹配）
     */
    private String itemName;

    /**
     * 订单状态（客如云原始枚举值，精确匹配；返回结果中已转换为中文）
     */
    private String orderStatus;

    /**
     * 订单类型（客如云原始枚举值，精确匹配；返回结果中已转换为中文）
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

    /**
     * 页码，从 1 开始；默认 1
     */
    private Integer pageNum = 1;

    /**
     * 每页条数；默认 20，最大 100
     */
    private Integer pageSize = 20;
}
