package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微智能表格 — 成员类型字段属性
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComUserFieldProperty {

    @JsonProperty("is_multiple")
    private Boolean isMultiple;
}
