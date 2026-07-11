package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 勤策分页查询通用请求体（订单 / 订单明细）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmPageQueryRequest {

    /**
     * 页码，从 1 开始
     */
    private String page;

    /**
     * 每页条数
     */
    private String rows;

    /**
     * 排序
     */
    private List<CrmSortItem> sorts;

    /**
     * 查询条件组
     */
    @JsonProperty("query_group")
    private List<CrmQueryGroup> queryGroup;
}
