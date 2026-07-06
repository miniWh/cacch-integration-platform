package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微智能表格 — 数字类型字段属性
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComNumberFieldProperty {

    @JsonProperty("decimal_places")
    private Integer decimalPlaces;

    @JsonProperty("use_separate")
    private Boolean useSeparate;
}
