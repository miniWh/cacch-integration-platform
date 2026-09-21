package com.cacch.integration.mapper.ihr;

import com.cacch.integration.entity.ihr.PersondetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * iHR persondetail 外部表 Mapper
 *
 * <p><strong>重要约束</strong>：{@code persondetail} 不在本项目 Flyway
 * 管理范围内，字段名已联调确认为 camelCase。本 Mapper <em>不继承 BaseMapper</em>，
 * 所有查询使用 {@code @Select} 手写原生 SQL，避免 MyBatis-Plus 自动注入
 * {@code is_deleted = 0} 等条件导致查不到数据。</p>
 *
 * <p>联调确认的列名（直接使用 camelCase，无需列别名）：
 * <ul>
 *     <li>{@code id} — 员工 ID → 映射 {@link PersondetailDO#getUserId}</li>
 *     <li>{@code staffNo} — 员工编号 → employeeNo</li>
 *     <li>{@code staffName} — 员工姓名 → userName</li>
 *     <li>{@code nickName} — 昵称 → nickname</li>
 *     <li>{@code workEmail} — 邮箱 → companyEmail</li>
 *     <li>{@code mobileNo} — 手机号 → contactPhone</li>
 *     <li>{@code departmentId} — 部门 ID（关联 organizationsdepartment.departmentcode）</li>
 *     <li>{@code staffStatus} — 在职状态（IN_SERVICE/QUIT）→ employeeStatus</li>
 * </ul>
 * 列名与 Java 字段名有差异的用列别名（如 {@code id AS userId}）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface PersondetailMapper {

    /**
     * 查询 persondetail 表全量员工记录
     *
     * <p>MyBatis 配置了 {@code map-underscore-to-camel-case: true}，
     * SQL 列别名必须使用 <strong>snake_case</strong>，才能被自动转驼峰
     * 匹配 Java DO 字段（如 employee_no → employeeNo）。
     * PG JDBC 老版本会把 AS 别名转小写，snake_case 天然是小写，不受影响。</p>
     *
     * <p>PostgreSQL 默认将未加引号的标识符存为小写，
     * 因此外部表的 camelCase 列名必须用双引号包裹才能正确引用。</p>
     *
     * @return persondetail 全量记录；无数据时返回空列表（非 null）
     */
    @Select("SELECT " +
            "id AS user_id, " +
            "\"staffNo\" AS employee_no, " +
            "\"staffName\" AS user_name, " +
            "\"nickName\" AS nickname, " +
            "\"workEmail\" AS company_email, " +
            "\"mobileNo\" AS contact_phone, " +
            "\"departmentId\" AS department_id, " +
            "\"staffStatus\" AS employee_status " +
            "FROM persondetail")
    List<PersondetailDO> selectAll();
}
