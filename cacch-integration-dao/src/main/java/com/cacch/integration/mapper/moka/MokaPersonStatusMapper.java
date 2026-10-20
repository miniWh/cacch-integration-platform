package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaPersonStatusDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Moka 人员入离职状态表 Mapper
 *
 * <p>基础 CRUD 继承自 {@link BaseMapper}；业务唯一键为 {@code employee_no}（工号），
 * 状态回写通过 PostgreSQL {@code INSERT ... ON CONFLICT (employee_no) DO UPDATE}
 * 原子语义实现，一人一行、幂等可重跑。主键 {@code id} 由 Service 层调用
 * {@code IdWorker} 预生成（手写 {@code @Update} 不触发 {@code ASSIGN_ID}）。</p>
 *
 * <p>待处理集合查询：以 {@code t_integration_moka_person} 为主表 LEFT JOIN 本状态表，
 * 入职待处理 = {@code employee_status='IN_SERVICE'} 且状态行缺失或
 * {@code hired_sync_status IN (0, 2)}；离职待处理 = {@code employee_status='QUIT'}
 * 且状态行缺失或 {@code resign_sync_status IN (0, 2)}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaPersonStatusMapper extends BaseMapper<MokaPersonStatusDO> {

    /**
     * 查询入职待处理集合
     *
     * <p>过滤条件：{@code t_integration_moka_person.employee_status = 'IN_SERVICE'}
     * 且 {@code employee_no} 非空，且状态行不存在或
     * {@code hired_sync_status IN (0, 2)}（PENDING + SYNC_FAILED）。
     * 不含已 SYNCED 记录，避免重复推送；含 {@code application_id}
     * （状态行存在时带回，机制确定后可直接使用）。</p>
     *
     * @return 入职待处理记录（按工号升序）；无数据时返回空列表（非 null）
     */
    @Select("SELECT " +
            "p.employee_no, " +
            "p.user_id, " +
            "p.user_name, " +
            "p.contact_phone, " +
            "p.employee_status, " +
            "s.application_id " +
            "FROM t_integration_moka_person p " +
            "LEFT JOIN t_integration_moka_person_status s " +
            "  ON s.employee_no = p.employee_no AND s.is_deleted = 0 " +
            "WHERE p.is_deleted = 0 " +
            "AND p.employee_no IS NOT NULL " +
            "AND p.employee_status = 'IN_SERVICE' " +
            "AND (s.id IS NULL OR s.hired_sync_status IN (0, 2)) " +
            "ORDER BY p.employee_no")
    List<MokaPersonStatusDO> listHiredPending();

    /**
     * 查询离职待处理集合
     *
     * <p>过滤条件：{@code t_integration_moka_person.employee_status = 'QUIT'}
     * 且 {@code employee_no} 非空，且状态行不存在或
     * {@code resign_sync_status IN (0, 2)}（PENDING + SYNC_FAILED）。
     * 不含已 SYNCED 记录，避免重复推送；含 {@code application_id}
     * （状态行存在时带回，机制确定后可直接使用）。</p>
     *
     * @return 离职待处理记录（按工号升序）；无数据时返回空列表（非 null）
     */
    @Select("SELECT " +
            "p.employee_no, " +
            "p.user_id, " +
            "p.user_name, " +
            "p.contact_phone, " +
            "p.employee_status, " +
            "s.application_id " +
            "FROM t_integration_moka_person p " +
            "LEFT JOIN t_integration_moka_person_status s " +
            "  ON s.employee_no = p.employee_no AND s.is_deleted = 0 " +
            "WHERE p.is_deleted = 0 " +
            "AND p.employee_no IS NOT NULL " +
            "AND p.employee_status = 'QUIT' " +
            "AND (s.id IS NULL OR s.resign_sync_status IN (0, 2)) " +
            "ORDER BY p.employee_no")
    List<MokaPersonStatusDO> listResignPending();

    /**
     * 回写入职同步结果 —— INSERT ON CONFLICT (employee_no) DO UPDATE
     *
     * <p>状态行不存在时 INSERT（application_id / hired 结果随行写入）；
     * 已存在时仅刷新冗余快照字段、application_id（入参为空时保留原值）、
     * hired_sync_status / hired_error / hired_sync_time，并复活逻辑删除行
     * （is_deleted 重置为 0），resign_* 字段保持原值不动。</p>
     *
     * @param id             内部主键（雪花预生成，ON CONFLICT 时不覆盖）
     * @param employeeNo     工号（业务唯一键）
     * @param userId         iHR 员工 ID
     * @param userName       姓名
     * @param contactPhone   手机号
     * @param applicationId  Moka 申请 id（允许为空，为空时保留原值）
     * @param employeeStatus 员工状态快照（IN_SERVICE/QUIT）
     * @param syncStatus     入职同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param error          失败原因摘要（允许为空）
     * @param syncTime       同步时间
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_moka_person_status " +
            "(id, employee_no, user_id, user_name, contact_phone, application_id, employee_status, " +
            "hired_sync_status, hired_error, hired_sync_time) " +
            "VALUES " +
            "(#{id}, #{employeeNo}, #{userId}, #{userName}, #{contactPhone}, #{applicationId}, #{employeeStatus}, " +
            "#{syncStatus}, #{error}, #{syncTime}) " +
            "ON CONFLICT (employee_no) DO UPDATE SET " +
            "user_id = EXCLUDED.user_id, " +
            "user_name = EXCLUDED.user_name, " +
            "contact_phone = EXCLUDED.contact_phone, " +
            "employee_status = EXCLUDED.employee_status, " +
            "application_id = COALESCE(#{applicationId}, t_integration_moka_person_status.application_id), " +
            "hired_sync_status = EXCLUDED.hired_sync_status, " +
            "hired_error = EXCLUDED.hired_error, " +
            "hired_sync_time = EXCLUDED.hired_sync_time, " +
            "is_deleted = 0, " +
            "updated_at = NOW()")
    int upsertHiredResult(@Param("id") Long id,
                          @Param("employeeNo") String employeeNo,
                          @Param("userId") String userId,
                          @Param("userName") String userName,
                          @Param("contactPhone") String contactPhone,
                          @Param("applicationId") Integer applicationId,
                          @Param("employeeStatus") String employeeStatus,
                          @Param("syncStatus") Integer syncStatus,
                          @Param("error") String error,
                          @Param("syncTime") LocalDateTime syncTime);

    /**
     * 回写离职同步结果 —— INSERT ON CONFLICT (employee_no) DO UPDATE
     *
     * <p>状态行不存在时 INSERT（application_id / resign 结果随行写入）；
     * 已存在时仅刷新冗余快照字段、application_id（入参为空时保留原值）、
     * resign_sync_status / resign_error / resign_sync_time，并复活逻辑删除行
     * （is_deleted 重置为 0），hired_* 字段保持原值不动。</p>
     *
     * @param id             内部主键（雪花预生成，ON CONFLICT 时不覆盖）
     * @param employeeNo     工号（业务唯一键）
     * @param userId         iHR 员工 ID
     * @param userName       姓名
     * @param contactPhone   手机号
     * @param applicationId  Moka 申请 id（允许为空，为空时保留原值）
     * @param employeeStatus 员工状态快照（IN_SERVICE/QUIT）
     * @param syncStatus     离职同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param error          失败原因摘要（允许为空）
     * @param syncTime       同步时间
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_moka_person_status " +
            "(id, employee_no, user_id, user_name, contact_phone, application_id, employee_status, " +
            "resign_sync_status, resign_error, resign_sync_time) " +
            "VALUES " +
            "(#{id}, #{employeeNo}, #{userId}, #{userName}, #{contactPhone}, #{applicationId}, #{employeeStatus}, " +
            "#{syncStatus}, #{error}, #{syncTime}) " +
            "ON CONFLICT (employee_no) DO UPDATE SET " +
            "user_id = EXCLUDED.user_id, " +
            "user_name = EXCLUDED.user_name, " +
            "contact_phone = EXCLUDED.contact_phone, " +
            "employee_status = EXCLUDED.employee_status, " +
            "application_id = COALESCE(#{applicationId}, t_integration_moka_person_status.application_id), " +
            "resign_sync_status = EXCLUDED.resign_sync_status, " +
            "resign_error = EXCLUDED.resign_error, " +
            "resign_sync_time = EXCLUDED.resign_sync_time, " +
            "is_deleted = 0, " +
            "updated_at = NOW()")
    int upsertResignResult(@Param("id") Long id,
                           @Param("employeeNo") String employeeNo,
                           @Param("userId") String userId,
                           @Param("userName") String userName,
                           @Param("contactPhone") String contactPhone,
                           @Param("applicationId") Integer applicationId,
                           @Param("employeeStatus") String employeeStatus,
                           @Param("syncStatus") Integer syncStatus,
                           @Param("error") String error,
                           @Param("syncTime") LocalDateTime syncTime);
}
