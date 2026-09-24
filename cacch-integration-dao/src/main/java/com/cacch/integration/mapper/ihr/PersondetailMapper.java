package com.cacch.integration.mapper.ihr;

import com.cacch.integration.entity.ihr.PersondetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    /**
     * 按 IHR 部门树递归查询在职员工（含 {@code ihr_staff_update_sync_record.last_update} 可选过滤）
     *
     * <p>SQL 执行流程：
     * <ol>
     *     <li>递归 CTE {@code dept_tree}：从 {@code ihr_dept_id} 锚点向下递归，
     *     收集所有子部门（{@code parent_id = 父.ihr_dept_id}），仅含 ENABLE 状态</li>
     *     <li>主查询：{@code persondetail} JOIN {@code dept_tree}（{@code p."departmentId"::varchar = t.ihr_dept_id}），
     *     只取在职员工（{@code staffStatus = 'IN_SERVICE'}）</li>
     *     <li>LEFT JOIN {@code ihr_staff_update_sync_record}（{@code staffId} 关联 {@code p.id}）
     *     附加 {@code last_update} 字段</li>
     *     <li>动态过滤：{@code lastUpdateDate} 非空时追加 {@code s.last_update >= #{lastUpdateDate}} 条件</li>
     * </ol>
     *
     * <p>返回 {@code List<Map<String, Object>>}：每个 Map 包含 persondetail 表所有字段（原列名 camelCase 保留）
     * + 部门字段（{@code department_name} / {@code department_code}）+ {@code staff_last_update}。
     * 使用 Map 承载避免 persondetail 表字段加减影响 Java 侧 VO 定义。</p>
     *
     * <p><strong>类型转换说明</strong>：
     * {@code persondetail."departmentId"} 为 bigint，{@code t_integration_ihr_department.ihr_dept_id} 为 varchar，
     * PG 严格类型校验需显式 {@code ::VARCHAR} 转换。</p>
     *
     * @param ihrDeptId      IHR 部门 ID（{@code t_integration_ihr_department.ihr_dept_id}），不可为空
     * @param lastUpdateDate 最后更新日期下界（可选，格式 yyyy-MM-dd）；为 null 时返回全部
     * @return 员工记录列表（每条为 Map，含 persondetail 所有字段 + 部门信息 + last_update）；无数据时返回空列表
     */
    @Select("<script>" +
            "WITH RECURSIVE dept_tree AS ( " +
            "  SELECT ihr_dept_id, name, department_code " +
            "  FROM t_integration_ihr_department " +
            "  WHERE ihr_dept_id = #{ihrDeptId} " +
            "    AND is_deleted = 0 " +
            "    AND department_status = 'ENABLE' " +
            "  UNION ALL " +
            "  SELECT d.ihr_dept_id, d.name, d.department_code " +
            "  FROM t_integration_ihr_department d " +
            "  INNER JOIN dept_tree t ON d.parent_id = t.ihr_dept_id " +
            "  WHERE d.is_deleted = 0 " +
            "    AND d.department_status = 'ENABLE' " +
            ") " +
            "SELECT p.*, " +
            "       t.name AS department_name, " +
            "       t.department_code AS department_code, " +
            "       s.last_update AS staff_last_update " +
            "FROM persondetail p " +
            "INNER JOIN dept_tree t ON p.\"departmentId\"::VARCHAR = t.ihr_dept_id " +
            "LEFT JOIN ihr_staff_update_sync_record s ON s.\"staffId\"::VARCHAR = p.\"id\"::VARCHAR " +
            "WHERE p.\"staffStatus\" = 'IN_SERVICE' " +
            "<if test='lastUpdateDate != null'> " +
            "  AND s.last_update &gt;= #{lastUpdateDate} " +
            "</if> " +
            "ORDER BY t.ihr_dept_id, p.\"staffNo\"" +
            "</script>")
    List<Map<String, Object>> selectByDeptTree(@Param("ihrDeptId") String ihrDeptId,
                                               @Param("lastUpdateDate") LocalDate lastUpdateDate);
}

