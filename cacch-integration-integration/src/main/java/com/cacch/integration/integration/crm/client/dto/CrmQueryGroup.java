package com.cacch.integration.integration.crm.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 勤策查询条件组
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmQueryGroup {

    /**
     * 组内连接符：AND / OR
     */
    private String connector;

    private List<CrmQueryFilter> filters;
}
