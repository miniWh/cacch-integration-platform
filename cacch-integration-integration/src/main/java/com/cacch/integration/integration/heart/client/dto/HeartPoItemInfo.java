package com.cacch.integration.integration.heart.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Heart PO 明细行解析结果
 *
 * <p>字段仅抄录 PO 原文，不做任何金额计算与编码转换。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeartPoItemInfo {

    /**
     * 行号
     */
    private Integer lineNo;

    /**
     * 产品名称（HeartSR 简称原文）
     */
    private String productName;

    /**
     * 产品编码（原文存在时抄录）
     */
    private String productCode;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 行金额
     */
    private BigDecimal amount;

    /**
     * 箱数
     */
    private Integer cartons;

    /**
     * 单箱量
     */
    private BigDecimal unitsPerCarton;

    /**
     * 体积
     */
    private BigDecimal volume;

    /**
     * 行备注
     */
    private String remark;
}
