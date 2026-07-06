package com.cacch.integration.integration.wecom.client.dto.smartsheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 企微智能表格 — 查询字段响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComGetFieldsResponse extends WeComBaseResponse {

    private Integer total;

    private List<WeComFieldInfo> fields;

    @JsonProperty("has_more")
    private Boolean hasMore;

    private Integer next;
}
