package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 企微智能表格 — 添加/更新记录中的单条记录
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
public class WeComRecordWriteItem {

    @JsonProperty("record_id")
    private String recordId;

    private Map<String, Object> values;
}
