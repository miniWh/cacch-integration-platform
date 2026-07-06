package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微智能表格 — 日期时间类型字段属性
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComDateTimeFieldProperty {

    private String format;

    @JsonProperty("auto_fill")
    private Boolean autoFill;
}
