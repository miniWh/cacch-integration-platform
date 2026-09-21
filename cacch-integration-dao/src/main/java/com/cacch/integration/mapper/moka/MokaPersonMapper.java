package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaPersonDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

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

    /**
     * 按 id 更新 Moka 同步状态字段
     *
     * <p>用于接口 2 推送 Moka 后回写同步结果：成功→1（SYNCED），
     * 失败→2（SYNC_FAILED）。{@code lastSyncTime} 由 Manager 层调用
     * {@link LocalDateTime#now()} 传入，{@code lastSyncResult}
     * 存放 Moka API 返回的 {@code msg} 摘要（失败时含 code+msg）。</p>
     *
     * <p>更新字段：{@code moka_sync_status / last_sync_time /
     * last_sync_result / updated_at}；不更新 {@code id / user_id} 等业务字段，
     * 不动 {@code is_deleted} 逻辑删除标记。</p>
     *
     * @param id         中间表主键
     * @param syncStatus 同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param syncTime   最近一次推送时间
     * @param syncResult 最近一次推送结果摘要（允许为空）
     * @return 受影响行数；id 不存在或已逻辑删除时返回 0
     */
    @Update("UPDATE t_integration_moka_person " +
            "SET moka_sync_status = #{syncStatus}, " +
            "    last_sync_time = #{syncTime}, " +
            "    last_sync_result = #{syncResult}, " +
            "    updated_at = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int updateSyncStatus(@Param("id") Long id,
                         @Param("syncStatus") Integer syncStatus,
                         @Param("syncTime") LocalDateTime syncTime,
                         @Param("syncResult") String syncResult);
}
