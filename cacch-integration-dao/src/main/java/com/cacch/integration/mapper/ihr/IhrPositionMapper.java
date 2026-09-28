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
     * <p>PostgreSQL 12+ 语法，使用 {@code EXCLUDED} 虚拟表引用待插入值。
     * 业务字段全部覆盖更新，雪花 id 在 uuid 冲突时保持原值不动，
     * 审计字段 {@code updated_at} 由 DO 的 MetaObjectHandler 自动填充。</p>
     *
     * @param d         职位 DO（必须包含 uuid）
     * @param syncBatch 本次同步批次号（写回确保与批次一致）
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_ihr_position (" +
            "id, uuid, company_id, position_name, abbreviation, position_code, " +
            "apply_range, capacity, affective_date, expiry_date, position_scope, " +
            "description, department_id, department_name, job_function, job_sub_function, " +
            "position_graded, position_grade_name, qualification, parent_id, " +
            "is_position_group, position_state, position_state_string, update_date, create_date, " +
            "sync_batch, created_at, updated_at, is_deleted" +
            ") VALUES (" +
            "#{d.id}, #{d.uuid}, #{d.companyId}, #{d.positionName}, #{d.abbreviation}, #{d.positionCode}, " +
            "#{d.applyRange}, #{d.capacity}, #{d.affectiveDate}, #{d.expiryDate}, #{d.positionScope}, " +
            "#{d.description}, #{d.departmentId}, #{d.departmentName}, #{d.jobFunction}, #{d.jobSubFunction}, " +
            "#{d.positionGraded}, #{d.positionGradeName}, #{d.qualification}, #{d.parentId}, " +
            "#{d.isPositionGroup}, #{d.positionState}, #{d.positionStateString}, #{d.updateDate}, #{d.createDate}, " +
            "#{syncBatch}, NOW(), NOW(), 0" +
            ") ON CONFLICT (uuid) DO UPDATE SET " +
            "company_id = EXCLUDED.company_id, " +
            "position_name = EXCLUDED.position_name, " +
            "abbreviation = EXCLUDED.abbreviation, " +
            "position_code = EXCLUDED.position_code, " +
            "apply_range = EXCLUDED.apply_range, " +
            "capacity = EXCLUDED.capacity, " +
            "affective_date = EXCLUDED.affective_date, " +
            "expiry_date = EXCLUDED.expiry_date, " +
            "position_scope = EXCLUDED.position_scope, " +
            "description = EXCLUDED.description, " +
            "department_id = EXCLUDED.department_id, " +
            "department_name = EXCLUDED.department_name, " +
            "job_function = EXCLUDED.job_function, " +
            "job_sub_function = EXCLUDED.job_sub_function, " +
            "position_graded = EXCLUDED.position_graded, " +
            "position_grade_name = EXCLUDED.position_grade_name, " +
            "qualification = EXCLUDED.qualification, " +
            "parent_id = EXCLUDED.parent_id, " +
            "is_position_group = EXCLUDED.is_position_group, " +
            "position_state = EXCLUDED.position_state, " +
            "position_state_string = EXCLUDED.position_state_string, " +
            "update_date = EXCLUDED.update_date, " +
            "create_date = EXCLUDED.create_date, " +
            "sync_batch = #{syncBatch}, " +
            "updated_at = NOW(), " +
            "is_deleted = 0"
    )
    int upsert(@Param("d") IhrPositionDO d, @Param("syncBatch") String syncBatch);
}
