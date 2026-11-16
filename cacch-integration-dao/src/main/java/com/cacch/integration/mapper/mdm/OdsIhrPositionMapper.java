package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.OdsIhrPositionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * iHR 岗位信息规范化 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface OdsIhrPositionMapper extends BaseMapper<OdsIhrPositionDO> {

    /**
     * UPSERT：INSERT ON CONFLICT (ihr_position_uuid) DO UPDATE SET ...
     *
     * @param o 规范化 DO（必须包含 ihrPositionUuid / rowHash / verNo / dqFlag）
     * @return 受影响行数
     */
    @Update("INSERT INTO ods_ihr_position (id, ihr_position_uuid, company_id, position_name, abbreviation, " +
            "position_code, applied_range, capacity, effective_date, description, department_id, department_name, " +
            "job_title_id, job_title_name, position_grade_id, position_grade_name, qualifications, parent_id, " +
            "is_position_group, position_state, position_state_string, updated_date, created_date, position_scope, " +
            "row_hash, ver_no, dq_flag, created_at, updated_at, is_deleted) " +
            "VALUES (#{o.id}, #{o.ihrPositionUuid}, #{o.companyId}, #{o.positionName}, #{o.abbreviation}, " +
            "#{o.positionCode}, #{o.appliedRange}, #{o.capacity}, #{o.effectiveDate}, #{o.description}, " +
            "#{o.departmentId}, #{o.departmentName}, #{o.jobTitleId}, #{o.jobTitleName}, #{o.positionGradeId}, " +
            "#{o.positionGradeName}, #{o.qualifications}, #{o.parentId}, #{o.isPositionGroup}, #{o.positionState}, " +
            "#{o.positionStateString}, #{o.updatedDate}, #{o.createdDate}, CAST(#{o.positionScope} AS JSONB), " +
            "#{o.rowHash}, #{o.verNo}, #{o.dqFlag}, NOW(), NOW(), 0) " +
            "ON CONFLICT (ihr_position_uuid) DO UPDATE SET " +
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
            "row_hash = EXCLUDED.row_hash, " +
            "ver_no = EXCLUDED.ver_no, " +
            "dq_flag = EXCLUDED.dq_flag, " +
            "updated_at = NOW(), " +
            "is_deleted = 0")
    int upsert(@Param("o") OdsIhrPositionDO o);
}
