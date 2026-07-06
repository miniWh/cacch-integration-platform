package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微智能表格 — 删除字段请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComDeleteFieldsRequest {

    private String docid;

    @JsonProperty("sheet_id")
    private String sheetId;

    @JsonProperty("field_ids")
    private List<String> fieldIds;
}
