package com.cacch.integration.mapper.ihr;

import com.cacch.integration.entity.ihr.OrganizationsdepartmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * organizationsdepartment 外部表 Mapper
 *
 * <p><strong>重要约束</strong>：{@code organizationsdepartment} 为外部业务库表
 * （与 persondetail 同属 iHR 外部库），不在本项目 Flyway 管理范围内。
 * 本 Mapper <em>不继承 BaseMapper</em>，所有查询使用 {@code @Select}
 * 手写原生 SQL，避免 MyBatis-Plus 自动注入条件。</p>
 *
 * <p>联调确认的列名：
 * <ul>
 *     <li>关联列：{@code id}（全小写，PG 原样存储，与 persondetail."departmentId" 对应）</li>
 *     <li>目标列：{@code departmentcode}（全小写，PG 原样存储）</li>
 * </ul>
 * </p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface OrganizationsdepartmentMapper {

    /**
     * 按部门 id 批量查询 departmentcode
     *
     * <p>用于 Moka 人员同步时将 persondetail."departmentId"（camelCase）
     * 批量映射为 organizationsdepartment.id（全小写）→ departmentcode。
     * SQL 使用 {@code IN} 批量查询，避免 N+1。</p>
     *
     * <p>列引用规则：
     * <ul>
     *   <li>外部表列名 {@code id} / {@code departmentcode} 均为全小写，
     *   PG 默认存为小写，不用双引号</li>
     *   <li>AS 别名用 snake_case（{@code department_id}），
     *   配合 MyBatis {@code map-underscore-to-camel-case=true}
     *   自动转驼峰匹配 DO 字段 {@code departmentId}</li>
     * </ul>
     * </p>
     *
     * @param departmentIds 部门 ID 列表（来自 persondetail.departmentId）
     * @return 匹配的部门 DO 列表；无数据时返回空列表（非 null）
     */
    @Select("<script>" +
            "SELECT id AS department_id, departmentcode FROM organizationsdepartment " +
            "WHERE id IN " +
            "<foreach collection='departmentIds' item='deptId' open='(' separator=',' close=')'>" +
            "#{deptId}" +
            "</foreach>" +
            "</script>")
    List<OrganizationsdepartmentDO> selectByDepartmentIds(List<String> departmentIds);
}
