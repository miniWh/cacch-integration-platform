package com.cacch.integration.service.crm.api;

import com.cacch.integration.common.dto.crm.CrmEmployeeDTO;

/**
 * 勤策员工帐号查询服务（对外产品化 API 底层）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmEmployeeQueryService {

    /**
     * 按勤策员工 ID 查询员工帐号
     *
     * @param employeeId 勤策员工唯一标识，不可为空
     * @return 员工信息；勤策成功但无数据时返回 null
     */
    CrmEmployeeDTO queryById(String employeeId);

    /**
     * 按 id / emp_id / emp_code 组合查询员工帐号
     *
     * <p>至少一项非空；与勤策约定一致时优先使用 {@code id}。</p>
     *
     * @param id      勤策员工 ID，可空
     * @param empId   第三方员工 ID（emp_id），可空
     * @param empCode 员工登录帐号（emp_code），可空
     * @return 员工信息；勤策成功但无数据时返回 null
     */
    CrmEmployeeDTO query(String id, String empId, String empCode);
}