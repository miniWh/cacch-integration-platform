package com.cacch.integration.entity.ihr;

import lombok.Data;

/**
 * iHR persondetail 表轻量 DO — Moka 人员同步所需字段
 *
 * <p>注意：{@code persondetail} 为外部业务库表，
 * 不在本项目 Flyway 管理范围内，字段名已按联调确认对齐：</p>
 *
 * <pre>
 *   DB 列        Java 字段       说明
 *   ─────────────────────────────────────
 *   id           userId          员工 ID（业务主键）
 *   staffNo      employeeNo      员工编号
 *   staffName    userName        员工姓名
 *   nickName     nickname        昵称
 *   workEmail    companyEmail    邮箱
 *   mobileNo     contactPhone    手机号
 *   departmentId departmentId    部门 ID（关联 organizationsdepartment.departmentcode）
 *   staffStatus  employeeStatus  在职状态：IN_SERVICE=在职 / QUIT=离职
 * </pre>
 *
 * <p>本类不使用 MyBatis-Plus 注解（@TableName / @TableId 等），
 * 避免被 BaseMapper 的自动逻辑删除 / 分页拦截器影响。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class PersondetailDO {

    /**
     * 员工 ID（persondetail.id，业务主键）
     */
    private String userId;

    /**
     * 员工编号（persondetail.staffNo）
     */
    private String employeeNo;

    /**
     * 员工姓名（persondetail.staffName）
     */
    private String userName;

    /**
     * 昵称（persondetail.nickName）
     */
    private String nickname;

    /**
     * 邮箱（persondetail.workEmail）
     */
    private String companyEmail;

    /**
     * 手机号（persondetail.mobileNo）
     */
    private String contactPhone;

    /**
     * 部门 ID（persondetail.departmentId，关联 organizationsdepartment.departmentcode）
     */
    private String departmentId;

    /**
     * 在职状态（persondetail.staffStatus）
     *
     * <p>取值：IN_SERVICE=在职，QUIT=离职</p>
     */
    private String employeeStatus;
}
