package com.cacch.integration.mapper.ihr;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.ihr.IhrPositionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * IHR 职位快照 Mapper
 *
 * <p>业务主键为 {@code uuid}（PostgreSQL UNIQUE 约束），
 * upsert 使用 {@code ON CONFLICT (uuid) DO UPDATE}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface IhrPositionMapper extends BaseMapper<IhrPositionDO> {

    /**
     * UPSERT：INSERT ON CONFLICT (uuid) DO UPDATE SET ...
     *
     * @param d         职位 DO（必须包含 uuid）
     * @param syncBatch 本次同步批次号（写回确保与批次一致）
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_ihr_position (" +
            "id, uuid, company_id, position_name, abbreviation, position_code, " +
            "applied_range, capacity, effective_date, description, " +
            "department_id, department_name, job_title_id, job_title_name, " +
            "position_grade_id, position_grade_name, qualifications, parent_id, " +
            "is_position_group, position_state, position_state_string, " +
            "updated_date, created_date, position_scope, " +
            "sync_batch, created_at, updated_at, is_deleted" +
            ") VALUES (" +
            "#{d.id}, #{d.uuid}, #{d.companyId}, #{d.positionName}, #{d.abbreviation}, #{d.positionCode}, " +
            "#{d.appliedRange}, #{d.capacity}, #{d.effectiveDate}, #{d.description}, " +
            "#{d.departmentId}, #{d.departmentName}, #{d.jobTitleId}, #{d.jobTitleName}, " +
            "#{d.positionGradeId}, #{d.positionGradeName}, #{d.qualifications}, #{d.parentId}, " +
            "#{d.isPositionGroup}, #{d.positionState}, #{d.positionStateString}, " +
            "#{d.updatedDate}, #{d.createdDate}, #{d.positionScope}::jsonb, " +
            "#{syncBatch}, NOW(), NOW(), 0" +
            ") ON CONFLICT (uuid) DO UPDATE SET " +
            "company_id = EXCLUDED.company_id, " +
            "position_name = EXCLUDED.position_name, " +
            "abbreviation = EXCLUDED.abbreviation, " +
            "position_code = EXCLUDED.position_code, " +
            "applied_range = EXCLUDED.applied_range, " +
            "capacity = EXCLUDED.capacity, " +
            "effective_date = EXCLUDED.effective_date, " +
            "description = EXCLUDED.description, " +
            "department_id = EXCLUDED.department_id, " +
            "department_name = EXCLUDED.department_name, " +
            "job_title_id = EXCLUDED.job_title_id, " +
            "job_title_name = EXCLUDED.job_title_name, " +
            "position_grade_id = EXCLUDED.position_grade_id, " +
            "position_grade_name = EXCLUDED.position_grade_name, " +
            "qualifications = EXCLUDED.qualifications, " +
            "parent_id = EXCLUDED.parent_id, " +
            "is_position_group = EXCLUDED.is_position_group, " +
            "position_state = EXCLUDED.position_state, " +
            "position_state_string = EXCLUDED.position_state_string, " +
            "updated_date = EXCLUDED.updated_date, " +
            "created_date = EXCLUDED.created_date, " +
            "position_scope = EXCLUDED.position_scope, " +
            "sync_batch = #{syncBatch}, " +
            "updated_at = NOW(), " +
            "is_deleted = 0"
    )
    int upsert(@Param("d") IhrPositionDO d, @Param("syncBatch") String syncBatch);
}
