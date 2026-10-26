package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaEntryApplicationDO;
import com.cacch.integration.entity.moka.MokaEntryPushItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

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
            "moka_job_id, moka_job_title, moka_pipeline_id, moka_pipeline_name, " +
            "moka_stage_id, moka_stage_name, moka_stage_type, moka_owner_id, moka_owner_name, " +
            "archived, archive_reason_id, archive_reason_type, archive_reason_name, archived_at, sync_batch) " +
            "VALUES " +
            "(#{id}, #{applicationId}, #{candidateId}, #{hireMode}, #{appliedAt}, #{movedAt}, #{mokaCreatedAt}, #{mokaUpdatedAt}, " +
            "#{mokaJobId}, #{mokaJobTitle}, #{mokaPipelineId}, #{mokaPipelineName}, " +
            "#{mokaStageId}, #{mokaStageName}, #{mokaStageType}, #{mokaOwnerId}, #{mokaOwnerName}, " +
            "#{archived}, #{archiveReasonId}, #{archiveReasonType}, #{archiveReasonName}, #{archivedAt}, #{syncBatch}) " +
            "ON CONFLICT (application_id) DO UPDATE SET " +
            "candidate_id = EXCLUDED.candidate_id, " +
            "hire_mode = EXCLUDED.hire_mode, " +
            "applied_at = EXCLUDED.applied_at, " +
            "moved_at = EXCLUDED.moved_at, " +
            "moka_created_at = EXCLUDED.moka_created_at, " +
            "moka_updated_at = EXCLUDED.moka_updated_at, " +
            "moka_job_id = EXCLUDED.moka_job_id, " +
            "moka_job_title = EXCLUDED.moka_job_title, " +
            "moka_pipeline_id = EXCLUDED.moka_pipeline_id, " +
            "moka_pipeline_name = EXCLUDED.moka_pipeline_name, " +
            "moka_stage_id = EXCLUDED.moka_stage_id, " +
            "moka_stage_name = EXCLUDED.moka_stage_name, " +
            "moka_stage_type = EXCLUDED.moka_stage_type, " +
            "moka_owner_id = EXCLUDED.moka_owner_id, " +
            "moka_owner_name = EXCLUDED.moka_owner_name, " +
            "archived = EXCLUDED.archived, " +
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
               @Param("mokaPipelineId") Integer mokaPipelineId,
               @Param("mokaPipelineName") String mokaPipelineName,
               @Param("mokaStageId") Integer mokaStageId,
               @Param("mokaStageName") String mokaStageName,
               @Param("mokaStageType") Integer mokaStageType,
               @Param("mokaOwnerId") Integer mokaOwnerId,
               @Param("mokaOwnerName") String mokaOwnerName,
               @Param("archived") Integer archived,
               @Param("archiveReasonId") Integer archiveReasonId,
               @Param("archiveReasonType") Integer archiveReasonType,
               @Param("archiveReasonName") String archiveReasonName,
               @Param("archivedAt") LocalDateTime archivedAt,
               @Param("syncBatch") String syncBatch);

    /**
     * Step 1b 回写：按 application_id 更新职位所属部门编码
     *
     * <p>使用业务键 {@code application_id}（upsert 冲突键，已有唯一约束）作为定位条件，
     * 避免依赖主键 {@code id} —— 后者在 ON CONFLICT DO UPDATE 分支不会刷新，
     * 导致按 id 更新时命中 0 行。</p>
     *
     * @param applicationId  Moka 申请 ID（业务唯一键，对应 t_integration_moka_entry_application.application_id）
     * @param departmentCode 部门编码（来自职位详情接口）
     * @return 受影响行数
     */
    @Update("UPDATE t_integration_moka_entry_application " +
            "SET moka_department_code = #{departmentCode}, " +
            "    updated_at = NOW() " +
            "WHERE application_id = #{applicationId} AND is_deleted = 0")
    int updateDepartmentCodeByApplicationId(@Param("applicationId") Integer applicationId,
                                            @Param("departmentCode") String departmentCode);

    /**
     * 按 application_id 更新 Offer 扩展字段（入职日期 + 薪资 + Offer 部门）
     *
     * <p>同 {@link #updateDepartmentCodeByApplicationId}，以业务键 {@code application_id}
     * 作为定位条件，避免主键 id 漂移导致 0 行更新。</p>
     *
     * @param applicationId       Moka 申请 ID（业务唯一键）
     * @param checkInDate         入职日期（ISO8601 或 yyyy-MM-dd）
     * @param salaryNumber        Offer 薪资（Moka salaryNumber 原始字符串，可能为 null）
     * @param offerDepartmentCode Offer 入职部门编码（来自 HCM）
     * @param offerDepartmentName Offer 入职部门名称（来自 HCM）
     * @param offerStatus         Offer 当前状态：pending/accepted/rejected/invalid/expired
     * @return 受影响行数
     */
    @Update("UPDATE t_integration_moka_entry_application " +
            "SET check_in_date = #{checkInDate}, " +
            "    salary_number = #{salaryNumber}, " +
            "    offer_department_code = #{offerDepartmentCode}, " +
            "    offer_department_name = #{offerDepartmentName}, " +
            "    offer_status = #{offerStatus}, " +
            "    updated_at = NOW() " +
            "WHERE application_id = #{applicationId} AND is_deleted = 0")
    int updateOfferFieldsByApplicationId(@Param("applicationId") Integer applicationId,
                                         @Param("checkInDate") LocalDateTime checkInDate,
                                         @Param("salaryNumber") String salaryNumber,
                                         @Param("offerDepartmentCode") String offerDepartmentCode,
                                         @Param("offerDepartmentName") String offerDepartmentName,
                                         @Param("offerStatus") String offerStatus);

    /**
     * 查询历史缺口：曾查到过 Offer 扩展数据但未回填薪资的申请 ID
     *
     * <p>补数范围定义：未删除 且 salary_number 为空 且（check_in_date 或 offer_status
     * 非空）—— 后者说明该申请曾完成 Step 1c 回写、确有 Offer 数据，避免对纯无 Offer
     * 的申请浪费 Moka 接口调用。按 application_id 升序保证分批稳定。</p>
     *
     * @return 缺失薪资的申请 ID 列表，无缺口时返回空列表（非 null）
     */
    @Select("SELECT application_id FROM t_integration_moka_entry_application " +
            "WHERE is_deleted = 0 " +
            "  AND salary_number IS NULL " +
            "  AND (check_in_date IS NOT NULL OR offer_status IS NOT NULL) " +
            "ORDER BY application_id ASC")
    List<Integer> listApplicationIdsMissingSalary();

    /**
     * 推送 iHR 前的联合查询：读取 4 表 + ihr_department JOIN 结果
     *
     * <p>一次查询拿全推送所需全部字段（候选人 + 职位/Offer + 学历 + 证件 + iHR 部门 ID），
     * 避免 N+1 查询。WHERE 条件包含同步状态筛选（默认 PENDING + FAILED）。</p>
     *
     * <p>iHR 部门 ID 通过
     * {@code ihr_department.department_code = moka_department_code} JOIN 获取，
     * CAST 为 BIGINT 以匹配 iHR {@code departmentId} 字段类型。</p>
     *
     * @param statusList 需要推送的同步状态列表（PENDING / FAILED / SKIPPED 等）
     * @param limit      单次查询上限
     * @return 推送项列表，按入职日期倒序
     */
    @Select("<script>" +
            "SELECT " +
            "  mea.application_id AS applicationId, " +
            "  mecb.moka_name AS name, " +
            "  mecb.moka_gender AS gender, " +
            "  mecb.moka_contact_phone AS phone, " +
            "  mecb.moka_contact_email AS email, " +
            "  mecb.moka_nationality AS nationality, " +
            "  mec.citizen_id AS citizenId, " +
            "  mec.certificate_type AS certificateType, " +
            "  mea.moka_department_code AS mokaDepartmentCode, " +
            "  mea.offer_department_name AS offerDepartmentName, " +
            "  mea.offer_department_code AS offerDepartmentCode, " +
            "  mea.check_in_date AS checkInDate, " +
            "  mea.offer_status AS offerStatus, " +
            "  mea.hire_mode AS hireMode, " +
            "  mea.moka_job_title AS mokaJobTitle, " +
            "  mea.moka_stage_name AS mokaStageName, " +
            "  mea.moka_owner_name AS mokaOwnerName, " +
            "  megs.last_school AS lastSchool, " +
            "  megs.last_speciality AS lastSpeciality, " +
            "  megs.academic_degree AS academicDegree, " +
            "  megs.graduate_date AS graduateDate, " +
            "  CAST(id.ihr_dept_id AS BIGINT) AS ihrDeptId, " +
            "  id.parent_id AS ihrParentId, " +
            "  mes.ihr_sync_status AS ihrSyncStatus, " +
            "  mes.ihr_entry_id AS ihrEntryId " +
            "FROM t_integration_moka_entry_application mea " +
            "LEFT JOIN t_integration_moka_entry_candidate_base mecb " +
            "  ON mea.application_id = mecb.application_id AND mecb.is_deleted = 0 " +
            "LEFT JOIN t_integration_moka_entry_graduate_school megs " +
            "  ON mea.application_id = megs.application_id AND megs.is_deleted = 0 " +
            "LEFT JOIN t_integration_moka_entry_citizen mec " +
            "  ON mea.application_id = mec.application_id AND mec.is_deleted = 0 " +
            "LEFT JOIN t_integration_moka_entry_sync mes " +
            "  ON mea.application_id = mes.application_id AND mes.is_deleted = 0 " +
            "LEFT JOIN t_integration_ihr_department id " +
            "  ON id.department_code = mea.moka_department_code " +
            "  AND id.is_deleted = 0 AND id.department_status = 'ENABLE' " +
            "WHERE mea.is_deleted = 0 " +
            "AND mes.ihr_sync_status IN " +
            "<foreach collection='statusList' item='s' open='(' separator=',' close=')'>" +
            "#{s}" +
            "</foreach> " +
            "ORDER BY mea.check_in_date DESC NULLS LAST " +
            "LIMIT #{limit}" +
            "</script>")
    List<MokaEntryPushItemVO> listForPush(@Param("statusList") List<String> statusList,
                                          @Param("limit") int limit);
}
