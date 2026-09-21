package com.cacch.integration.entity.ihr;

import lombok.Data;

/**
 * organizationsdepartment 表轻量 DO — 部门关联
 *
 * <p>注意：{@code organizationsdepartment} 为外部业务库表，
 * 与 {@code persondetail} 同属 iHR 外部库，不在本项目 Flyway 管理范围内。
 * 列名假设与 persondetail 同为 camelCase，联调时若发现差异直接改 SQL 列别名即可。</p>
 *
 * <p>本类不使用 MyBatis-Plus 注解，避免被 BaseMapper 自动条件影响。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OrganizationsdepartmentDO {

    /**
     * 部门 ID（与 persondetail.departmentId 关联，外部表列名假设为 departmentId）
     */
    private String departmentId;

    /**
     * 部门 code（Moka API departmentCode 入参；外部表列名假设为 departmentcode，
     * 联调确认后可能需改为 departmentCode / department_code）
     */
    private String departmentcode;
}
