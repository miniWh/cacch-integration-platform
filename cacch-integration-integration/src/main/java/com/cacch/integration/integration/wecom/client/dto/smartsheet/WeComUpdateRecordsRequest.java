package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微智能表格 — 更新记录请求
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComUpdateRecordsRequest {

    private String docid;

    @JsonProperty("sheet_id")
    private String sheetId;

    @JsonProperty("key_type")
    private String keyType;

    private List<WeComRecordWriteItem> records;
}
