package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企微智能表格字段信息
 *
 * @author cacch-integration
 */
@Data
public class WeComFieldInfo {

    @JsonProperty("field_id")
    private String fieldId;

    @JsonProperty("field_title")
    private String fieldTitle;

    @JsonProperty("field_type")
    private String fieldType;
}
