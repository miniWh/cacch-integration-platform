package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaPersonDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Moka 人员信息中间表 Mapper
 *
 * <p>基础 CRUD 继承自 {@link BaseMapper}；业务唯一键为 {@code user_id}，
 * upsert 通过 PostgreSQL {@code INSERT ... ON CONFLICT (user_id) DO UPDATE}
 * 原子语义实现。主键 {@code id} 由 Service 层调用 {@code IdWorker}
 * 预生成（手写 {@code @Update} 不触发 {@code ASSIGN_ID}）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaPersonMapper extends BaseMapper<MokaPersonDO> {

    /**
     * UPSERT：INSERT ON CONFLICT (user_id) DO UPDATE
     *
     * <p>以 {@code user_id} 为冲突检测键，已存在时覆盖业务字段
     * （employee_no / user_name / nickname / company_email / contact_phone /
     * role_id / department_code / employee_status / deactivated）。
     * 主键 {@code id} 在 DO UPDATE 时不覆盖，保持幂等。</p>
     *
     * <p>未传入的列（locale / timezone / moka_sync_status / superior_email 等）
     * 在 INSERT 时由 DDL DEFAULT 填充，DO UPDATE 时保持原值不动。
     * superior_email 当前版本不写入（Moka API updateSuperiorEmail=false）。</p>
     *
     * @param id             内部主键（雪花预生成）
     * @param userId         iHR 员工 ID（业务主键，UNIQUE）
     * @param employeeNo     工号
     * @param userName       姓名
     * @param nickname       昵称
     * @param companyEmail   工作邮箱
     * @param contactPhone   工作电话
     * @param roleId         Moka 角色 ID（阶段一默认 223379）
     * @param departmentCode 部门编号（从 organizationsdepartment 关联得到）
     * @param employeeStatus 员工状态
     * @param deactivated    是否禁用（0/1）
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_moka_person " +
            "(id, user_id, employee_no, user_name, nickname, company_email, contact_phone, " +
            "role_id, department_code, employee_status, deactivated) " +
            "VALUES " +
            "(#{id}, #{userId}, #{employeeNo}, #{userName}, #{nickname}, #{companyEmail}, #{contactPhone}, " +
            "#{roleId}, #{departmentCode}, #{employeeStatus}, #{deactivated}) " +
            "ON CONFLICT (user_id) DO UPDATE SET " +
            "employee_no = EXCLUDED.employee_no, " +
            "user_name = EXCLUDED.user_name, " +
            "nickname = EXCLUDED.nickname, " +
            "company_email = EXCLUDED.company_email, " +
            "contact_phone = EXCLUDED.contact_phone, " +
            "role_id = EXCLUDED.role_id, " +
            "department_code = EXCLUDED.department_code, " +
            "employee_status = EXCLUDED.employee_status, " +
            "deactivated = EXCLUDED.deactivated")
    int upsert(@Param("id") Long id,
               @Param("userId") String userId,
               @Param("employeeNo") String employeeNo,
               @Param("userName") String userName,
               @Param("nickname") String nickname,
               @Param("companyEmail") String companyEmail,
               @Param("contactPhone") String contactPhone,
               @Param("roleId") Integer roleId,
               @Param("departmentCode") String departmentCode,
               @Param("employeeStatus") String employeeStatus,
               @Param("deactivated") Integer deactivated);
}
