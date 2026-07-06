package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微智能表格 — 添加字段请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComAddFieldsRequest {

    private String docid;

    @JsonProperty("sheet_id")
    private String sheetId;

    private List<WeComFieldAddItem> fields;
}
