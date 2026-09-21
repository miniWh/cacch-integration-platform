package com.cacch.integration.mapper.ihr;

import com.cacch.integration.entity.ihr.PersondetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * iHR persondetail 外部表 Mapper
 *
 * <p><strong>重要约束</strong>：{@code persondetail} 不在本项目 Flyway
 * 管理范围内，字段名、索引、是否存在逻辑删除字段均未知。
 * 因此本 Mapper <em>不继承 BaseMapper</em>，所有查询使用 {@code @Select}
 * 手写原生 SQL，避免 MyBatis-Plus 自动注入 {@code is_deleted = 0}
 * 等条件导致查不到数据。</p>
 *
 * <p>联调前需确认：
 * <ul>
 *     <li>表名（{@code persondetail} 为假定值）</li>
 *     <li>字段名（userId / employeeNo / departmentId 等是否加下划线前缀）</li>
 *     <li>主键是否为 userId</li>
 * </ul>
 * 若有差异，直接修改 SQL 即可。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface PersondetailMapper {

    /**
     * 查询 persondetail 表全量员工记录
     *
     * <p>用于 Moka 人员同步接口 1 的数据来源。本表为外部业务库表，
     * 字段名按约定对齐：userId / employeeNo / name / nickname /
     * companyEmail / phone / jobTitle / departmentId / superiorsInfo / employeeStatus。
     * 若联调发现字段名差异，直接修改列别名适配即可。</p>
     *
     * @return persondetail 全量记录；无数据时返回空列表（非 null）
     */
    @Select("SELECT " +
            "user_id, " +
            "employee_no, " +
            "name, " +
            "nickname, " +
            "company_email, " +
            "phone, " +
            "job_title, " +
            "department_id, " +
            "superiors_info, " +
            "employee_status " +
            "FROM persondetail")
    List<PersondetailDO> selectAll();
}
