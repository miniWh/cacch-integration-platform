package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 企微智能表格 — 查询记录响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComGetRecordsResponse extends WeComBaseResponse {

    private Integer total;

    @JsonProperty("has_more")
    private Boolean hasMore;

    private Integer next;

    private List<WeComRecordInfo> records;

    private Integer ver;
}
