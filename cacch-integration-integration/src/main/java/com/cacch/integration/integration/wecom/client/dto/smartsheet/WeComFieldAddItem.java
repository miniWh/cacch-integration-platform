package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微智能表格 — 添加字段项
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeComFieldAddItem {

    @JsonProperty("field_title")
    private String fieldTitle;

    @JsonProperty("field_type")
    private String fieldType;

    @JsonProperty("property_user")
    private WeComUserFieldProperty propertyUser;
}
