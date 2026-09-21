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
 * <p>联调假设（若有差异直接改 SQL 列别名即可）：
 * <ul>
 *     <li>关联列：{@code departmentId}（与 persondetail.departmentId 对应）</li>
 *     <li>目标列：{@code departmentcode}（用户给定，联调若为 departmentCode
 *     则改 {@code departmentCode AS departmentcode}）</li>
 * </ul>
 * </p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface OrganizationsdepartmentMapper {

    /**
     * 按 departmentId 批量查询部门 code
     *
     * <p>用于 Moka 人员同步时将 persondetail.departmentId 批量映射为
     * departmentcode。SQL 使用 {@code IN} 批量查询，避免 N+1。</p>
     *
     * @param departmentIds 部门 ID 列表
     * @return 匹配的部门 DO 列表；无数据时返回空列表（非 null）
     */
    @Select("<script>" +
            "SELECT departmentId, departmentcode FROM organizationsdepartment " +
            "WHERE departmentId IN " +
            "<foreach collection='departmentIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<OrganizationsdepartmentDO> selectByDepartmentIds(List<String> departmentIds);
}
