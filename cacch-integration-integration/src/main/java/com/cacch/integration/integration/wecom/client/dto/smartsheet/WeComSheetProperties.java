package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企微智能表格 — 子表属性
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComSheetProperties {

    @JsonProperty("sheet_id")
    private String sheetId;

    private String title;

    private Integer index;
}
