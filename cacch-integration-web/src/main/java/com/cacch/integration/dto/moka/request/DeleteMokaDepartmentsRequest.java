package com.cacch.integration.dto.moka.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Moka 部门手动删除 —— web 层请求体
 *
 * <p>传入待删除部门的编码列表，由 Manager 层校验存在性、标记待删除后
 * 立即调用 Moka 删除及合并组织架构接口。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class DeleteMokaDepartmentsRequest {

    /**
     * 待删除部门编码列表（必填，至少一条；编码对应本地表 t_integration_moka_department.department_code）
     */
    @NotEmpty(message = "部门编码列表至少包含一条记录")
    @Valid
    private List<@NotBlank(message = "部门编码不可为空") String> departmentCodes;
}
