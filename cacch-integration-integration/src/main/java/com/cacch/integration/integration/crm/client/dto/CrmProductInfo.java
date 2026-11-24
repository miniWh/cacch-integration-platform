package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 勤策商品信息，对应查询商品响应的 response_data 元素
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmProductInfo {

    /**
     * 商品唯一标识
     */
    @JsonProperty("prd_id")
    private String prdId;

    /**
     * 商品状态：1 正常、0 删除
     */
    @JsonProperty("status")
    private String status;

    /**
     * 勤策商品 ID
     */
    @JsonProperty("prd_waiqin365_id")
    private String prdWaiqin365Id;

    /**
     * 商品编码
     */
    @JsonProperty("prd_code")
    private String prdCode;

    /**
     * 助记码
     */
    @JsonProperty("prd_short_code")
    private String prdShortCode;

    /**
     * 条码
     */
    @JsonProperty("prd_barcode")
    private String prdBarcode;

    /**
     * 商品名称
     */
    @JsonProperty("prd_name")
    private String prdName;

    /**
     * 分类 ID
     */
    @JsonProperty("class_id")
    private String classId;

    /**
     * 分类名称
     */
    @JsonProperty("class_name")
    private String className;

    /**
     * 分类来源编码
     */
    @JsonProperty("class_source_code")
    private String classSourceCode;

    /**
     * 分类全路径
     */
    @JsonProperty("class_full_path")
    private String classFullPath;

    /**
     * 规格
     */
    @JsonProperty("prd_spec")
    private String prdSpec;

    /**
     * 品牌
     */
    @JsonProperty("prd_brand")
    private String prdBrand;

    /**
     * 基本单位名称
     */
    @JsonProperty("prd_unit")
    private String prdUnit;

    /**
     * 保质期
     */
    @JsonProperty("prd_valid_period")
    private Integer prdValidPeriod;

    /**
     * 排序值
     */
    @JsonProperty("prd_sequ")
    private BigDecimal prdSequ;

    /**
     * 备注
     */
    @JsonProperty("prd_remarks")
    private String prdRemarks;

    /**
     * 图片地址
     */
    @JsonProperty("pictures")
    private String pictures;

    /**
     * 创建时间
     */
    @JsonProperty("create_time")
    private String createTime;

    /**
     * 新品标签
     */
    @JsonProperty("with_tag_new")
    private String withTagNew;

    /**
     * 热销标签
     */
    @JsonProperty("with_tag_hot")
    private String withTagHot;

    /**
     * 赠品标签
     */
    @JsonProperty("with_tag_gift")
    private String withTagGift;

    /**
     * 促销标签
     */
    @JsonProperty("with_tag_sale")
    private String withTagSale;

    /**
     * 扩展标签一
     */
    @JsonProperty("with_tag_ex1")
    private String withTagEx1;

    /**
     * 建议零售价
     */
    @JsonProperty("prd_suggest_price")
    private BigDecimal prdSuggestPrice;

    /**
     * 成本价
     */
    @JsonProperty("prd_cost_price")
    private BigDecimal prdCostPrice;

    /**
     * 售价
     */
    @JsonProperty("prd_price")
    private BigDecimal prdPrice;

    /**
     * 销售状态
     */
    @JsonProperty("sale_status")
    private String saleStatus;

    /**
     * 商品简称
     */
    @JsonProperty("prd_short_name")
    private String prdShortName;

    /**
     * 标准箱系数
     */
    @JsonProperty("prd_teu_coefficient")
    private BigDecimal prdTeuCoefficient;

    /**
     * 托盘系数
     */
    @JsonProperty("prd_tray_coefficient")
    private BigDecimal prdTrayCoefficient;

    /**
     * 同价码
     */
    @JsonProperty("prd_same_price_code")
    private String prdSamePriceCode;

    /**
     * 创建人 ID
     */
    @JsonProperty("creator_id")
    private String creatorId;

    /**
     * 创建人名称
     */
    @JsonProperty("create_name")
    private String createName;

    /**
     * 修改人 ID
     */
    @JsonProperty("modifyier_id")
    private String modifyierId;

    /**
     * 修改人名称
     */
    @JsonProperty("modifyier_name")
    private String modifyierName;

    /**
     * 修改时间
     */
    @JsonProperty("modifier_time")
    private String modifierTime;

    /**
     * 税率
     */
    @JsonProperty("prd_tax_rate")
    private BigDecimal prdTaxRate;

    /**
     * 是否合并商品
     */
    @JsonProperty("prd_is_merge")
    private String prdIsMerge;

    /**
     * 商品分类：0 普通、1 助销物料、2 包装物、3 兑换物料
     */
    @JsonProperty("classification")
    private String classification;

    /**
     * 门店上架状态
     */
    @JsonProperty("store_shelf_status")
    private String storeShelfStatus;

    /**
     * 基本单位净重
     */
    @JsonProperty("base_unit_net_weight")
    private BigDecimal baseUnitNetWeight;

    /**
     * 基本单位净重计量单位编码
     */
    @JsonProperty("base_unit_net_weight_code")
    private String baseUnitNetWeightCode;

    /**
     * 上报单位 ID
     */
    @JsonProperty("prd_report_unit_id")
    private String prdReportUnitId;

    /**
     * 采购单位 ID
     */
    @JsonProperty("prd_purchase_unit_id")
    private String prdPurchaseUnitId;

    /**
     * 上报单位名称
     */
    @JsonProperty("prd_report_unit_name")
    private String prdReportUnitName;

    /**
     * 采购单位名称
     */
    @JsonProperty("prd_purchase_unit_name")
    private String prdPurchaseUnitName;

    /**
     * 来源编码
     */
    @JsonProperty("source_code")
    private String sourceCode;

    /**
     * 商品名称拼音
     */
    @JsonProperty("prd_name_py")
    private String prdNamePy;

    /**
     * 自定义字段列表
     */
    @JsonProperty("prd_exts")
    private List<CrmProductExtInfo> prdExts;

    /**
     * 单位列表
     */
    @JsonProperty("prd_units")
    private List<CrmProductUnitInfo> prdUnits;
}
