package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 勤策商品自定义字段，对应响应中的 prd_exts 元素
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmProductExtInfo {

    /**
     * 自定义字段标识
     */
    @JsonProperty("prd_ext_key")
    private String prdExtKey;

    /**
     * 自定义字段值
     */
    @JsonProperty("prd_ext_value")
    private String prdExtValue;
}
