package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 勤策单条过滤条件
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmQueryFilter {

    @JsonProperty("field_key")
    private String fieldKey;

    /**
     * 操作符，如 EQ / GE / LE / ISN 等
     */
    private String operator;

    @JsonProperty("field_values")
    private List<String> fieldValues;
}
