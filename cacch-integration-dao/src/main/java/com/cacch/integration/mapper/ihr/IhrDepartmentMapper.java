package com.cacch.integration.mapper.ihr;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.ihr.IhrDepartmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * IHR 部门快照 Mapper
 *
 * <p>业务主键为 {@code uuid}（PostgreSQL UNIQUE 约束），
 * upsert 使用 {@code ON CONFLICT (uuid) DO UPDATE}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface IhrDepartmentMapper extends BaseMapper<IhrDepartmentDO> {

    /**
     * UPSERT：INSERT ON CONFLICT (uuid) DO UPDATE SET ...
     *
     * <p>PostgreSQL 12+ 语法，使用 {@code EXCLUDED} 虚拟表引用待插入值。
     * 业务字段全部覆盖更新，雪花 id 在 uuid 冲突时保持原值不动（避免覆盖），
     * 审计字段 {@code updated_at} 由 DO 的 MetaObjectHandler 自动填充。</p>
     *
     * @param d         部门 DO（必须包含 uuid）
     * @param syncBatch 本次同步批次号（写回确保与批次一致）
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_ihr_department (" +
            "id, uuid, ihr_dept_id, name, parent_id, type, department_code, store_number, " +
            "principal_staff_id, parent_department_code, parent_department_name, virtual, " +
            "department_status, department_desc, department_property, last_update, " +
            "created_date, abbreviation, establish_date, effective_date, remark, sequence, " +
            "sync_batch, created_at, updated_at, is_deleted" +
            ") VALUES (" +
            "#{d.id}, #{d.uuid}, #{d.ihrDeptId}, #{d.name}, #{d.parentId}, #{d.type}, " +
            "#{d.departmentCode}, #{d.storeNumber}, #{d.principalStaffId}, " +
            "#{d.parentDepartmentCode}, #{d.parentDepartmentName}, #{d.virtual}, " +
            "#{d.departmentStatus}, #{d.departmentDesc}, #{d.departmentProperty}, #{d.lastUpdate}, " +
            "#{d.createdDate}, #{d.abbreviation}, #{d.establishDate}, #{d.effectiveDate}, " +
            "#{d.remark}, #{d.sequence}, #{syncBatch}, NOW(), NOW(), 0" +
            ") ON CONFLICT (uuid) DO UPDATE SET " +
            "ihr_dept_id = EXCLUDED.ihr_dept_id, " +
            "name = EXCLUDED.name, " +
            "parent_id = EXCLUDED.parent_id, " +
            "type = EXCLUDED.type, " +
            "department_code = EXCLUDED.department_code, " +
            "store_number = EXCLUDED.store_number, " +
            "principal_staff_id = EXCLUDED.principal_staff_id, " +
            "parent_department_code = EXCLUDED.parent_department_code, " +
            "parent_department_name = EXCLUDED.parent_department_name, " +
            "virtual = EXCLUDED.virtual, " +
            "department_status = EXCLUDED.department_status, " +
            "department_desc = EXCLUDED.department_desc, " +
            "department_property = EXCLUDED.department_property, " +
            "last_update = EXCLUDED.last_update, " +
            "created_date = EXCLUDED.created_date, " +
            "abbreviation = EXCLUDED.abbreviation, " +
            "establish_date = EXCLUDED.establish_date, " +
            "effective_date = EXCLUDED.effective_date, " +
            "remark = EXCLUDED.remark, " +
            "sequence = EXCLUDED.sequence, " +
            "sync_batch = #{syncBatch}, " +
            "updated_at = NOW(), " +
            "is_deleted = 0"
    )
    int upsert(@Param("d") IhrDepartmentDO d, @Param("syncBatch") String syncBatch);
}
