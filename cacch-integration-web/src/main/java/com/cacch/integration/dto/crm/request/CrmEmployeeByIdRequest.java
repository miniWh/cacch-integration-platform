package com.cacch.integration.dto.crm.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按员工 ID 查询员工帐号请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class CrmEmployeeByIdRequest {

    /**
     * 勤策员工唯一标识（订单 owner.id）
     */
    @NotBlank(message = "employeeId 不能为空")
    private String employeeId;
}
