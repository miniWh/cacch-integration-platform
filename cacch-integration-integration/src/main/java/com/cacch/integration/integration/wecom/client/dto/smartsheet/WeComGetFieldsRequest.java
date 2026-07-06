package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微智能表格 — 查询字段请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeComGetFieldsRequest {

    private String docid;

    @JsonProperty("sheet_id")
    private String sheetId;

    @JsonProperty("view_id")
    private String viewId;

    @JsonProperty("field_ids")
    private List<String> fieldIds;

    @JsonProperty("field_titles")
    private List<String> fieldTitles;

    private Integer offset;

    private Integer limit;
}
