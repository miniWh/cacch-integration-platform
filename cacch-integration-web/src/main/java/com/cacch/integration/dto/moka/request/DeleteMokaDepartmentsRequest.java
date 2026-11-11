package com.cacch.integration.dto.moka.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Moka 部门手动删除 —— web 层请求体
 *
 * <p>departmentCodes 非空时按传入编码手动删除；为空（不传或空数组）时自动按
 * iHR 快照中停用（department_status='DISABLE'）的部门全集执行删除。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class DeleteMokaDepartmentsRequest {

    /**
     * 待删除部门编码列表（可选；编码对应本地表 t_integration_moka_department.department_code）。
     * 为 null 或空数组时触发自动模式：按 iHR 停用部门自动执行删除
     */
    @Valid
    private List<@NotBlank(message = "部门编码不可为空") String> departmentCodes;
}
