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
 * <p>待处理集合查询驱动链路：
 * <pre>
 *   t_integration_moka_person (员工主数据)
 *     INNER JOIN t_integration_moka_entry_candidate_base (待入职候选人)
 *       ON p.contact_phone = c.moka_contact_phone  —— 手机号关联，Moka 域内部
 *     LEFT JOIN t_integration_moka_person_status (状态表)
 *       ON s.employee_no = p.employee_no AND s.is_deleted = 0
 * </pre>
 * INNER JOIN candidate_base 天然过滤掉「无候选人记录」的员工，
 * 确保只有在 Moka ATS 待入职快照中有对应 application_id 的候选人
 * 才会进入待处理集合并触发 hired / resign 调用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaPersonStatusMapper extends BaseMapper<MokaPersonStatusDO> {

    /**
     * 查询入职待处理集合
     *
     * <p>过滤条件：
     * <ul>
     *     <li>{@code t_integration_moka_person.employee_status = 'IN_SERVICE'}</li>
     *     <li>INNER JOIN candidate_base：{@code contact_phone = moka_contact_phone}，
     *     天然排除无候选人记录的员工</li>
     *     <li>工号非空 + 状态行缺失或 {@code hired_sync_status IN (0, 2)}</li>
     * </ul></p>
     *
     * @return 入职待处理记录（按工号升序）；无数据时返回空列表（非 null）
     */
    @Select("SELECT " +
            "p.employee_no, " +
            "p.user_id, " +
            "p.user_name, " +
            "p.contact_phone, " +
            "p.employee_status, " +
            "c.application_id " +
            "FROM t_integration_moka_person p " +
            "INNER JOIN t_integration_moka_entry_candidate_base c " +
            "  ON c.moka_contact_phone = p.contact_phone AND c.is_deleted = 0 " +
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
     * <p>过滤条件：
     * <ul>
     *     <li>{@code t_integration_moka_person.employee_status = 'QUIT'}</li>
     *     <li>INNER JOIN candidate_base：{@code contact_phone = moka_contact_phone}，
     *     天然排除无候选人记录的员工</li>
     *     <li>工号非空 + 状态行缺失或 {@code resign_sync_status IN (0, 2)}</li>
     * </ul></p>
     *
     * @return 离职待处理记录（按工号升序）；无数据时返回空列表（非 null）
     */
    @Select("SELECT " +
            "p.employee_no, " +
            "p.user_id, " +
            "p.user_name, " +
            "p.contact_phone, " +
            "p.employee_status, " +
            "c.application_id " +
            "FROM t_integration_moka_person p " +
            "INNER JOIN t_integration_moka_entry_candidate_base c " +
            "  ON c.moka_contact_phone = p.contact_phone AND c.is_deleted = 0 " +
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
     * <p>状态行不存在时 INSERT；已存在时仅刷新 application_id（入参为空时保留原值）、
     * hired_sync_status / hired_error / hired_sync_time，并复活逻辑删除行
     * （is_deleted 重置为 0），resign_* 字段保持原值不动。</p>
     *
     * @param id            内部主键（雪花预生成，ON CONFLICT 时不覆盖）
     * @param employeeNo    工号（业务唯一键）
     * @param applicationId Moka 申请 id（来自 candidate_base JOIN，允许为空）
     * @param syncStatus    入职同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param error         失败原因摘要（允许为空）
     * @param syncTime      同步时间
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_moka_person_status " +
            "(id, employee_no, application_id, " +
            "hired_sync_status, hired_error, hired_sync_time) " +
            "VALUES " +
            "(#{id}, #{employeeNo}, #{applicationId}, " +
            "#{syncStatus}, #{error}, #{syncTime}) " +
            "ON CONFLICT (employee_no) DO UPDATE SET " +
            "application_id = COALESCE(#{applicationId}, t_integration_moka_person_status.application_id), " +
            "hired_sync_status = EXCLUDED.hired_sync_status, " +
            "hired_error = EXCLUDED.hired_error, " +
            "hired_sync_time = EXCLUDED.hired_sync_time, " +
            "is_deleted = 0, " +
            "updated_at = NOW()")
    int upsertHiredResult(@Param("id") Long id,
                          @Param("employeeNo") String employeeNo,
                          @Param("applicationId") Integer applicationId,
                          @Param("syncStatus") Integer syncStatus,
                          @Param("error") String error,
                          @Param("syncTime") LocalDateTime syncTime);

    /**
     * 回写离职同步结果 —— INSERT ON CONFLICT (employee_no) DO UPDATE
     *
     * <p>状态行不存在时 INSERT；已存在时仅刷新 application_id（入参为空时保留原值）、
     * resign_sync_status / resign_error / resign_sync_time，并复活逻辑删除行
     * （is_deleted 重置为 0），hired_* 字段保持原值不动。</p>
     *
     * @param id            内部主键（雪花预生成，ON CONFLICT 时不覆盖）
     * @param employeeNo    工号（业务唯一键）
     * @param applicationId Moka 申请 id（来自 candidate_base JOIN，允许为空）
     * @param syncStatus    离职同步状态：1=SYNCED, 2=SYNC_FAILED
     * @param error         失败原因摘要（允许为空）
     * @param syncTime      同步时间
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_moka_person_status " +
            "(id, employee_no, application_id, " +
            "resign_sync_status, resign_error, resign_sync_time) " +
            "VALUES " +
            "(#{id}, #{employeeNo}, #{applicationId}, " +
            "#{syncStatus}, #{error}, #{syncTime}) " +
            "ON CONFLICT (employee_no) DO UPDATE SET " +
            "application_id = COALESCE(#{applicationId}, t_integration_moka_person_status.application_id), " +
            "resign_sync_status = EXCLUDED.resign_sync_status, " +
            "resign_error = EXCLUDED.resign_error, " +
            "resign_sync_time = EXCLUDED.resign_sync_time, " +
            "is_deleted = 0, " +
            "updated_at = NOW()")
    int upsertResignResult(@Param("id") Long id,
                           @Param("employeeNo") String employeeNo,
                           @Param("applicationId") Integer applicationId,
                           @Param("syncStatus") Integer syncStatus,
                           @Param("error") String error,
                           @Param("syncTime") LocalDateTime syncTime);
}
