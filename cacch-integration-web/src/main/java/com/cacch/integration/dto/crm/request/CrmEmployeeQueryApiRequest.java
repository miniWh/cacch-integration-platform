package com.cacch.integration.dto.crm.request;

import lombok.Data;

/**
 * 员工帐号组合查询请求（对外 API）
 *
 * <p>{@code id}、{@code empId}、{@code empCode} 至少一项非空，由服务端校验。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class CrmEmployeeQueryApiRequest {

    /**
     * 勤策员工唯一标识
     */
    private String id;

    /**
     * 第三方员工唯一标识（勤策 emp_id）
     */
    private String empId;

    /**
     * 员工登录帐号（勤策 emp_code）
     */
    private String empCode;
}