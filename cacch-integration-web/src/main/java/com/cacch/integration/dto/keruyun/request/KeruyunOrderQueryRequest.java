package com.cacch.integration.dto.keruyun.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客如云订单查询请求参数
 *
 * <p>所有过滤条件均为可选；未传条件时按 openTime 倒序返回。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderQueryRequest {

    /**
     * 客如云订单 ID（精确匹配，唯一）
     */
    private String orderId;

    /**
     * 门店 ID（精确匹配）
     */
    private String shopId;

    /**
     * 订单状态（精确匹配，可选多个）
     */
    private String orderStatus;

    /**
     * 订单类型（精确匹配，可选多个）
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
     * 记录创建时间起始（含）
     */
    private LocalDateTime createdAtFrom;

    /**
     * 记录创建时间结束（含）
     */
    private LocalDateTime createdAtTo;

    /**
     * 页码，从 1 开始；默认 1
     */
    private Integer pageNum = 1;

    /**
     * 每页条数；默认 20，最大 100
     */
    private Integer pageSize = 20;
}
