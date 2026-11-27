package com.cacch.integration.integration.heart.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Heart PO 结构化解析结果主体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeartPoInfo {

    /**
     * PO 号
     */
    private String poNo;

    /**
     * PO 日期（原文）
     */
    private String poDate;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 贸易条款
     */
    private String tradeTerms;

    /**
     * 起运港
     */
    private String portOfLoading;

    /**
     * 目的港
     */
    private String portOfDischarge;

    /**
     * 船公司
     */
    private String shippingLine;

    /**
     * 柜型
     */
    private String containerType;

    /**
     * 包装方式
     */
    private String packingType;

    /**
     * 预计开船日（原文）
     */
    private String etd;

    /**
     * 币种
     */
    private String currency;

    /**
     * PO 总额
     */
    private BigDecimal totalAmount;

    /**
     * 整单备注
     */
    private String remark;

    /**
     * 明细行
     */
    private List<HeartPoItemInfo> items;
}
