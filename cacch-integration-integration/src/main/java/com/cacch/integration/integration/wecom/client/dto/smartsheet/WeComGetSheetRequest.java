package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微智能表格 — 查询子表请求
 *
 * @author cacch-integration
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeComGetSheetRequest {

    private String docid;

    @JsonProperty("sheet_id")
    private String sheetId;

    @JsonProperty("need_all_type_sheet")
    private Boolean needAllTypeSheet;
}
