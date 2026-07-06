package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企微智能表格子表信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComSheetInfo {

    @JsonProperty("sheet_id")
    private String sheetId;

    private String title;

    @JsonProperty("is_visible")
    private Boolean visible;

    /**
     * 子表类型：smartsheet / dashboard / external
     */
    private String type;
}
