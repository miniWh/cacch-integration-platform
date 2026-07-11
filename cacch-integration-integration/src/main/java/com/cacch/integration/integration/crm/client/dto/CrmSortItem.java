package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 勤策排序项
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmSortItem {

    @JsonProperty("field_key")
    private String fieldKey;

    /**
     * asc / desc
     */
    private String type;
}
