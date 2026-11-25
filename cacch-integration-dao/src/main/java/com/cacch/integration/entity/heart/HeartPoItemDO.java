package com.cacch.integration.entity.heart;

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
 * Heart 客户 PO 明细行 DO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_crm_heart_po_item")
public class HeartPoItemDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单主表 ID
     */
    private Long poId;

    /**
     * 行号
     */
    private Integer lineNo;

    private String productName;

    private String spec;

    /**
     * CRM 物料编码
     */
    private String pdCode;

    /**
     * CRM 产品对象 ID
     */
    private String productId;

    /**
     * 箱数
     */
    private BigDecimal cartonCount;

    /**
     * L
     */
    private BigDecimal liters;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 总价
     */
    private BigDecimal amount;

    private String auxText;

    private BigDecimal density;

    private BigDecimal netWeight;

    private BigDecimal netWeightWithCarton;

    private BigDecimal volume;

    private BigDecimal cartonGrossWeight;

    private BigDecimal grossWeight;

    private BigDecimal emptyCartonSets;

    private BigDecimal grossWeightWithExtraCarton;

    /**
     * 成本价格（CNY/L）
     */
    private BigDecimal costPrice;

    /**
     * 不打托单价
     */
    private BigDecimal noPalletUnitPrice;

    /**
     * 包干费（元）
     */
    private BigDecimal packageFee;

    /**
     * 中信保（元）
     */
    private BigDecimal creditInsuranceFee;

    /**
     * 其它杂费（元）
     */
    private BigDecimal otherFee;

    /**
     * 费用总计（元）
     */
    private BigDecimal costTotal;

    private BigDecimal grossProfit;

    private BigDecimal grossMarginRate;

    private String remark;

    /**
     * 数据来源：AI/MANUAL/COMPUTED
     */
    private String dataSource;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
