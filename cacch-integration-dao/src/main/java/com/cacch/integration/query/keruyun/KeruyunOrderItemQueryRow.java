package com.cacch.integration.query.keruyun;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客如云订单菜品明细查询行（SKU 维度）
 *
 * <p>非表实体：仅用于 KeruyunOrderItemMapper 自定义查询 SQL 的结果承载，
 * 一行 = 一个订单 × 一个 SKU。订单状态/类型已在 SQL 中转换为中文，
 * 金额字段已在 SQL 中从分转换为元（保留 2 位小数）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class KeruyunOrderItemQueryRow {

    /**
     * 客如云订单 ID
     */
    private String orderId;

    /**
     * 门店 ID
     */
    private String shopId;

    /**
     * 业务订单号
     */
    private String busiOrderNo;

    /**
     * 订单状态（中文展示值，未识别的枚举返回原始值）
     */
    private String orderStatus;

    /**
     * 订单类型（中文展示值，未识别的枚举返回原始值）
     */
    private String orderType;

    /**
     * 下单时间
     */
    private LocalDateTime openTime;

    /**
     * 完结时间
     */
    private LocalDateTime finishTime;

    /**
     * SKU 编码，对应 stg_keruyun_order_item.item_code
     */
    private String itemCode;

    /**
     * 菜品名称
     */
    private String itemName;

    /**
     * 菜品大类
     */
    private String bigTypeName;

    /**
     * 菜品中类
     */
    private String midTypeName;

    /**
     * 单位
     */
    private String unitName;

    /**
     * 规格（拼接展示值 spec_name_concat）
     */
    private String specName;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 是否赠菜：true-赠菜 false-非赠菜（对应表列 gift_flag，BOOLEAN）
     */
    private Boolean giftFlag;

    /**
     * 售卖状态码：NORMAL-销售 DISCARD-退菜
     */
    private String saleStatusTypeCode;

    /**
     * 原始单价（元，2 位小数）
     */
    private BigDecimal itemPriceYuan;

    /**
     * 售价（元，2 位小数）
     */
    private BigDecimal salePriceYuan;

    /**
     * 销售金额（元，2 位小数）
     */
    private BigDecimal itemSaleAmtYuan;
}
