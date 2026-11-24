package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 勤策新增销售合同明细行
 *
 * <p>仅建模 openAPI 可写入字段；金额、折扣、BOM 节点等只读字段由勤策自动计算，不在此声明。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmContractDetailItem {

    /**
     * 商品 ID；与 pdCode、pdSourceCode 至少传一个，优先 pdId / pdCode
     */
    private Long pdId;

    /**
     * 商品编码；与 pdId、pdSourceCode 至少传一个
     */
    private String pdCode;

    /**
     * 商品第三方编码；与 pdId、pdCode 至少传一个
     */
    private String pdSourceCode;

    /**
     * 单位 ID；未启用多单位时由系统赋值，启用多单位时与 unitName 二选一
     */
    private Long unitId;

    /**
     * 单位名称；优先使用 unitId
     */
    private String unitName;

    /**
     * 商品数量，必须大于 0
     */
    private BigDecimal pdCount;

    /**
     * 实际售价，必须大于等于 0；金额由系统按 实际售价 × 数量 计算
     */
    private BigDecimal actualPrice;

    /**
     * 商品组合 ID；与 pdPortfolioCode 二选一，仅支持标准 BOM
     */
    private Long pdPortfolioId;

    /**
     * 商品组合编码；与 pdPortfolioId 二选一，仅支持标准 BOM
     */
    private String pdPortfolioCode;

    /**
     * 明细自定义字段，格式 {字段key: 字段值}
     */
    private Map<String, Object> extFields;
}
