package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 勤策商品单位信息，对应响应中的 prd_units 元素
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmProductUnitInfo {

    /**
     * 是否基本单位
     */
    @JsonProperty("prd_is_base")
    private String prdIsBase;

    /**
     * 换算比例
     */
    @JsonProperty("prd_ratio")
    private BigDecimal prdRatio;

    /**
     * 条码
     */
    @JsonProperty("prd_barcode")
    private String prdBarcode;

    /**
     * 单位名称
     */
    @JsonProperty("prd_unit_name")
    private String prdUnitName;

    /**
     * 勤策单位 ID（文档样例字段名末尾带空格，已做别名兼容）
     */
    @JsonProperty("prd_unit_waiqin365_id")
    @JsonAlias("prd_unit_waiqin365_id ")
    private String prdUnitWaiqin365Id;

    /**
     * 单位重量
     */
    @JsonProperty("prd_weight")
    private String prdWeight;

    /**
     * 重量单位
     */
    @JsonProperty("prd_weight_unit")
    private String prdWeightUnit;
}
