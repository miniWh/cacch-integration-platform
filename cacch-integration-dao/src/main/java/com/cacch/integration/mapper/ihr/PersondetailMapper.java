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
     * <p>列名已联调确认为 camelCase，id 列用别名 AS userId 匹配 Java DO。
     * 注意：SQL 中 {@code id} 列映射为 Java 的 {@code userId}（业务主键）。</p>
     *
     * @return persondetail 全量记录；无数据时返回空列表（非 null）
     */
    @Select("SELECT " +
            "id AS userId, " +
            "staffNo AS employeeNo, " +
            "staffName AS userName, " +
            "nickName AS nickname, " +
            "workEmail AS companyEmail, " +
            "mobileNo AS contactPhone, " +
            "departmentId, " +
            "staffStatus AS employeeStatus " +
            "FROM persondetail")
    List<PersondetailDO> selectAll();
}
