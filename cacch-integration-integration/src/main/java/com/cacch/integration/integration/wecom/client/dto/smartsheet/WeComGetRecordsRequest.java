package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微智能表格 — 查询记录请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeComGetRecordsRequest {

    private String docid;

    @JsonProperty("sheet_id")
    private String sheetId;

    @JsonProperty("view_id")
    private String viewId;

    @JsonProperty("record_ids")
    private List<String> recordIds;

    @JsonProperty("key_type")
    private String keyType;

    @JsonProperty("field_titles")
    private List<String> fieldTitles;

    @JsonProperty("field_ids")
    private List<String> fieldIds;

    private Integer offset;

    private Integer limit;

    private Integer ver;
}
