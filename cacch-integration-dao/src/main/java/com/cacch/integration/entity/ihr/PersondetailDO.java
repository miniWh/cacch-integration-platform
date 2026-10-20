package com.cacch.integration.entity.ihr;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /**
     * 合同开始日期（persondetail."contractBeginDate"，实际入职时间）
     *
     * <p>Moka hired 接口 hiredAt 数据来源；列类型按日期联调核实</p>
     */
    private LocalDate contractBeginDate;

    /**
     * 试用期结束日期（persondetail."probationEndDate"）
     *
     * <p>与 contractBeginDate 的整月差值即 Moka hired 接口 probation 数据来源</p>
     */
    private LocalDate probationEndDate;

    /**
     * 离职日期时间（persondetail."leaveDate"）
     *
     * <p>Moka resign 接口 time 数据来源；列类型按日期时间联调核实</p>
     */
    private LocalDateTime leaveDate;
}
