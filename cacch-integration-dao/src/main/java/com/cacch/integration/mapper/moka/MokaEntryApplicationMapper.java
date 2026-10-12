package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaEntryApplicationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * Moka 待入职候选人 —— 申请主表 Mapper
 *
 * <p>基础 CRUD 继承自 {@link BaseMapper}。幂等写入通过
 * PostgreSQL {@code INSERT ... ON CONFLICT (application_id) DO UPDATE}
 * 原子语义实现（手写 @Update，主键 id 由 Service 层预生成）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaEntryApplicationMapper extends BaseMapper<MokaEntryApplicationDO> {

    /**
     * UPSERT：按 application_id 幂等写入主表
     *
     * @param id 内部主键（雪花预生成）
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_moka_entry_application " +
            "(id, application_id, candidate_id, hire_mode, applied_at, moved_at, moka_created_at, moka_updated_at, " +
            "moka_job_id, moka_job_title, moka_job_department, moka_pipeline_id, moka_pipeline_name, " +
            "moka_stage_id, moka_stage_name, moka_stage_type, moka_owner_id, moka_owner_name, " +
            "archive_reason_id, archive_reason_type, archive_reason_name, archived_at, sync_batch) " +
            "VALUES " +
            "(#{id}, #{applicationId}, #{candidateId}, #{hireMode}, #{appliedAt}, #{movedAt}, #{mokaCreatedAt}, #{mokaUpdatedAt}, " +
            "#{mokaJobId}, #{mokaJobTitle}, #{mokaJobDepartment}, #{mokaPipelineId}, #{mokaPipelineName}, " +
            "#{mokaStageId}, #{mokaStageName}, #{mokaStageType}, #{mokaOwnerId}, #{mokaOwnerName}, " +
            "#{archiveReasonId}, #{archiveReasonType}, #{archiveReasonName}, #{archivedAt}, #{syncBatch}) " +
            "ON CONFLICT (application_id) DO UPDATE SET " +
            "candidate_id = EXCLUDED.candidate_id, " +
            "hire_mode = EXCLUDED.hire_mode, " +
            "applied_at = EXCLUDED.applied_at, " +
            "moved_at = EXCLUDED.moved_at, " +
            "moka_created_at = EXCLUDED.moka_created_at, " +
            "moka_updated_at = EXCLUDED.moka_updated_at, " +
            "moka_job_id = EXCLUDED.moka_job_id, " +
            "moka_job_title = EXCLUDED.moka_job_title, " +
            "moka_job_department = EXCLUDED.moka_job_department, " +
            "moka_pipeline_id = EXCLUDED.moka_pipeline_id, " +
            "moka_pipeline_name = EXCLUDED.moka_pipeline_name, " +
            "moka_stage_id = EXCLUDED.moka_stage_id, " +
            "moka_stage_name = EXCLUDED.moka_stage_name, " +
            "moka_stage_type = EXCLUDED.moka_stage_type, " +
            "moka_owner_id = EXCLUDED.moka_owner_id, " +
            "moka_owner_name = EXCLUDED.moka_owner_name, " +
            "archive_reason_id = EXCLUDED.archive_reason_id, " +
            "archive_reason_type = EXCLUDED.archive_reason_type, " +
            "archive_reason_name = EXCLUDED.archive_reason_name, " +
            "archived_at = EXCLUDED.archived_at, " +
            "sync_batch = EXCLUDED.sync_batch, " +
            "updated_at = NOW()")
    int upsert(@Param("id") Long id,
               @Param("applicationId") Integer applicationId,
               @Param("candidateId") Integer candidateId,
               @Param("hireMode") Integer hireMode,
               @Param("appliedAt") LocalDateTime appliedAt,
               @Param("movedAt") LocalDateTime movedAt,
               @Param("mokaCreatedAt") LocalDateTime mokaCreatedAt,
               @Param("mokaUpdatedAt") LocalDateTime mokaUpdatedAt,
               @Param("mokaJobId") String mokaJobId,
               @Param("mokaJobTitle") String mokaJobTitle,
               @Param("mokaJobDepartment") String mokaJobDepartment,
               @Param("mokaPipelineId") Integer mokaPipelineId,
               @Param("mokaPipelineName") String mokaPipelineName,
               @Param("mokaStageId") Integer mokaStageId,
               @Param("mokaStageName") String mokaStageName,
               @Param("mokaStageType") Integer mokaStageType,
               @Param("mokaOwnerId") Integer mokaOwnerId,
               @Param("mokaOwnerName") String mokaOwnerName,
               @Param("archiveReasonId") Integer archiveReasonId,
               @Param("archiveReasonType") Integer archiveReasonType,
               @Param("archiveReasonName") String archiveReasonName,
               @Param("archivedAt") LocalDateTime archivedAt,
               @Param("syncBatch") String syncBatch);

    /**
     * Step 1b 回写：按 id 更新职位所属部门编码
     *
     * @param id             主表主键
     * @param departmentCode 部门编码（来自职位详情接口）
     * @return 受影响行数
     */
    @Update("UPDATE t_integration_moka_entry_application " +
            "SET moka_department_code = #{departmentCode}, " +
            "    updated_at = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int updateDepartmentCodeById(@Param("id") Long id,
                                 @Param("departmentCode") String departmentCode);

    /**
     * Step 1c 回写：按 id 更新 Offer 扩展字段（入职日期 + Offer 部门）
     *
     * @param id                  主表主键
     * @param checkInDate         入职日期（ISO8601 或 yyyy-MM-dd）
     * @param offerDepartmentCode Offer 入职部门编码（来自 HCM）
     * @param offerDepartmentName Offer 入职部门名称（来自 HCM）
     * @param offerStatus         Offer 当前状态：pending/accepted/rejected/invalid/expired
     * @return 受影响行数
     */
    @Update("UPDATE t_integration_moka_entry_application " +
            "SET check_in_date = #{checkInDate}, " +
            "    offer_department_code = #{offerDepartmentCode}, " +
            "    offer_department_name = #{offerDepartmentName}, " +
            "    offer_status = #{offerStatus}, " +
            "    updated_at = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int updateOfferFieldsById(@Param("id") Long id,
                              @Param("checkInDate") LocalDateTime checkInDate,
                              @Param("offerDepartmentCode") String offerDepartmentCode,
                              @Param("offerDepartmentName") String offerDepartmentName,
                              @Param("offerStatus") String offerStatus);
}
