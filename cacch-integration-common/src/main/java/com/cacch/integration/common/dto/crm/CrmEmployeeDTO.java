package com.cacch.integration.common.dto.crm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 勤策员工帐号查询结果（服务层）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmEmployeeDTO {

    /**
     * 勤策员工唯一标识
     */
    private String id;

    /**
     * 第三方系统员工唯一标识（勤策 emp_id）
     */
    private String empId;

    /**
     * 员工登录帐号（勤策 emp_code）
     */
    private String empCode;

    /**
     * 员工姓名
     */
    private String empName;

    /**
     * 手机号（勤策有返回时映射）
     */
    private String mobile;

    /**
     * 在职状态（勤策有返回时映射）
     */
    private String status;
}