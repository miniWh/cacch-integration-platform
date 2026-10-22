package com.cacch.integration.integration.keruyun.client.dto.order;

import lombok.Data;

/**
 * 客如云订单列表元素
 *
 * <p>字段与客如云订单列表查询接口响应 {@code result.data.list[]} 逐字一致；
 * 金额单位均为分（字符串），时间为 {@code yyyy-MM-dd HH:mm:ss}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderItem {

    /**
     * 品牌 ID
     */
    private String brandId;

    /**
     * 门店 ID
     */
    private String shopId;

    /**
     * 订单 ID（本地落库业务唯一键）
     */
    private String orderId;

    /**
     * 业务订单号
     */
    private String busiOrderNo;

    /**
     * 订单状态：WAIT_PROCESSED / ORDER_RECEIVED / SUCCESS / WAIT_SETTLED / SETTLED /
     * REFUND / CLOSED / INVALID / CANCELLED / REJECTED / ANTI_SETTLED
     */
    private String orderStatus;

    /**
     * 订单类型：FOR_HERE / PLATFORM_TAKE_OUT / SELF_TAKE_OUT / SELF_TAKE /
     * NO_ORDER_CASHIER / MEMBER_STORE / MEMBER_MANUAL_STORE / REPAYMENT_ORDER
     */
    private String orderType;

    /**
     * 订单金额（单位：分）
     */
    private String orderAmt;

    /**
     * 优惠金额（单位：分）
     */
    private String promoAmt;

    /**
     * 订单收入/订单实收（单位：分）
     */
    private String orderReceivedAmt;

    /**
     * 下单时间
     */
    private String openTime;

    /**
     * 完结时间
     */
    private String finishTime;

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
     * 订单模式标签：REV_POSITIVE_ORDER / REV_REVERSE_ORDER / REV_RESULT_ORDER / ORG_RESULT_ORDER
     */
    private String orderModeTag;

    /**
     * 订单模式：ORIGINAL_ORDER / REVERSE_ORDER / HISTORY
     */
    private String orderMode;
}
