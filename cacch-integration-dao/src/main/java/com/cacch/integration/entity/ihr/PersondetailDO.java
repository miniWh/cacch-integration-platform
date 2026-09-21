package com.cacch.integration.entity.ihr;

import lombok.Data;

/**
 * iHR persondetail 表轻量 DO — 仅包含 Moka 人员同步所需字段
 *
 * <p>注意：{@code persondetail} 为外部业务库表，
 * 不在本项目 Flyway 管理范围内，字段名按 iHR 数据库实际结构对齐。
 * 若联调时发现字段名差异（如 {@code userId} 实际叫 {@code user_id}），
 * 需在 {@link com.cacch.integration.mapper.ihr.PersondetailMapper} 的
 * 手写 SQL 中用列别名适配。</p>
 *
 * <p>本类不使用 MyBatis-Plus 注解（@TableName / @TableId 等），
 * 避免被 BaseMapper 的自动逻辑删除 / 分页拦截器影响。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class PersondetailDO {

    /**
     * iHR 员工 ID（业务主键）
     */
    private String userId;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 姓名
     */
    private String name;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 工作邮箱
     */
    private String companyEmail;

    /**
     * 工作电话
     */
    private String phone;

    /**
     * 职位（阶段二用于匹配 Moka 角色）
     */
    private String jobTitle;

    /**
     * 部门 ID（iHR 侧，关联 ihr_department.ihr_dept_id）
     */
    private String departmentId;

    /**
     * 上级信息（JSON 字符串，需解析提取直属领导邮箱）
     */
    private String superiorsInfo;

    /**
     * 员工状态（iHR 原始值）
     */
    private String employeeStatus;
}
