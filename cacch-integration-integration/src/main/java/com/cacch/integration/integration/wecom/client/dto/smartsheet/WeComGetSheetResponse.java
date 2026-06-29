package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 企微智能表格 — 查询子表响应
 *
 * @author cacch-integration
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComGetSheetResponse extends WeComBaseResponse {

    @JsonProperty("sheet_list")
    private List<WeComSheetInfo> sheetList;
}
