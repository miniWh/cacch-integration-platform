package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微智能表格 — 单选类型字段属性
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComSingleSelectFieldProperty {

    @JsonProperty("is_quick_add")
    private Boolean isQuickAdd;

    private List<WeComSelectOption> options;
}
