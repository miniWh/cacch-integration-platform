package com.cacch.integration.integration.crm.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 勤策查询员工帐号请求体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmEmployeeQueryRequest {

    /**
     * 勤策员工唯一标识（优先）
     */
    private String id;

    /**
     * 第三方系统员工唯一标识
     */
    @JsonProperty("emp_id")
    private String empId;

    /**
     * 员工登录帐号
     */
    @JsonProperty("emp_code")
    private String empCode;
}
