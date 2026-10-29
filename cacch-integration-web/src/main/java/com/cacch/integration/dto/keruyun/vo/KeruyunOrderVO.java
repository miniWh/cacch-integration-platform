package com.cacch.integration.dto.keruyun.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客如云订单视图对象
 *
 * <p>金额字段已从 DO 的分（Long）转换为元（BigDecimal，保留 2 位小数）。
 * 如需与客如云原始金额精确对比，注意此处单位已变化。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderVO {

    /**
     * 内部主键
     */
    private Long id;

    /**
     * 品牌 ID
     */
    private String brandId;

    /**
     * 门店 ID
     */
    private String shopId;

    /**
     * 客如云订单 ID，业务唯一键
     */
    private String orderId;

    /**
     * 业务订单号
     */
    private String busiOrderNo;

    /**
     * 订单状态，取值见客如云订单状态枚举
     */
    private String orderStatus;

    /**
     * 订单类型，取值见客如云订单类型枚举
     */
    private String orderType;

    /**
     * 订单金额（单位：元，保留 2 位小数）
     */
    private BigDecimal orderAmtYuan;

    /**
     * 优惠金额（单位：元，保留 2 位小数）
     */
    private BigDecimal promoAmtYuan;

    /**
     * 订单收入 / 实收（单位：元，保留 2 位小数）
     */
    private BigDecimal orderReceivedAmtYuan;

    /**
     * 下单时间
     */
    private LocalDateTime openTime;

    /**
     * 完结时间
     */
    private LocalDateTime finishTime;

    /**
     * 第三方订单号
     */
    private String thirdOrderNo;

    /**
     * 订单流水号
     */
    private String serialNo;

    /**
     * 第三方流水号
     */
    private String thirdSerialNo;

    /**
     * 订单模式标签
     */
    private String orderModeTag;

    /**
     * 订单模式
     */
    private String orderMode;

    /**
     * 同步批次号
     */
    private String syncBatch;

    /**
     * 最近一次从客如云同步成功的时间
     */
    private LocalDateTime syncedAt;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    private LocalDateTime updatedAt;
}
