package com.cacch.integration.entity.keruyun;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客如云订单菜品明细落库实体，映射表 stg_keruyun_order_item
 *
 * <p>字段对应订单详情接口 orderItemVoList 元素，金额单位为分；
 * 套餐子商品通过 parentItemRowId 关联父行，随父行平铺存储。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("stg_keruyun_order_item")
public class KeruyunOrderItemDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 客如云订单 ID，关联 stg_keruyun_order.order_id
     */
    private String orderId;

    /**
     * 客如云商品行 ID（文档字段 id），订单内唯一
     */
    private String itemRowId;

    /**
     * 父商品行 ID（套餐子商品场景，关联本表 itemRowId）
     */
    private String parentItemRowId;

    /**
     * 商品档案 ID（文档字段 itemId）
     */
    private String itemId;

    /**
     * 规格商品 ID
     */
    private String itemSkuId;

    /**
     * 商品类型：SINGLE_DISH/SINGLE_SET/COMBO/CUSTOMER_SET 等
     */
    private String itemType;

    /**
     * 商品编码
     */
    private String itemCode;

    /**
     * 商品名称
     */
    private String itemName;

    /**
     * 商品大类名称
     */
    private String bigTypeName;

    /**
     * 商品中类名称
     */
    private String midTypeName;

    /**
     * 单位名称
     */
    private String unitName;

    /**
     * 规格
     */
    private String specName;

    /**
     * 规格名称全称
     */
    private String specNameConcat;

    /**
     * 出品部门 ID
     */
    private String productionDeptId;

    /**
     * 商品售卖状态：NORMAL-销售 / DISCARD-退菜
     */
    private String saleStatusType;

    /**
     * 商品售卖状态编码：NORMAL/DISCARD
     */
    private String saleStatusTypeCode;

    /**
     * 是否赠送
     */
    private Boolean giftFlag;

    /**
     * 是否称重商品
     */
    private Boolean weighFlag;

    /**
     * 是否临时菜
     */
    private Boolean tempFlag;

    /**
     * 是否优惠菜
     */
    private Boolean promoFlag;

    /**
     * 销售/退菜/赠菜数量
     */
    private BigDecimal quantity;

    /**
     * 商品原始单价（单位：分）
     */
    private Long itemPrice;

    /**
     * 商品售价（单位：分）
     */
    private Long salePrice;

    /**
     * 商品销售金额（单位：分）
     */
    private Long itemSaleAmt;

    /**
     * 服务费分摊金额（单位：分）
     */
    private Long extraFeeApportionAmt;

    /**
     * 商品优惠分摊（单位：分）
     */
    private Long itemPromoApportionAmt;

    /**
     * 商品收入（单位：分）
     */
    private Long itemReceivedAmt;

    /**
     * 做法信息（扁平化，格式：做法名:金额(分)，分号分隔）
     */
    private String practiceInfo;

    /**
     * 商品备注
     */
    private String itemSubject;

    /**
     * 菜品退款时间（仅外卖退款有值）
     */
    private LocalDateTime itemRefundTime;

    /**
     * 商品税率（如 1.53%）
     */
    private String taxRate;

    /**
     * 单品税额（单位：分）
     */
    private Long itemActualTaxAmt;

    /**
     * 菜品行税额（单位：分）
     */
    private Long actualTaxAmt;

    /**
     * 记录创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记：0-正常 1-删除
     */
    @TableLogic
    private Integer isDeleted;
}
