package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.OdsIhrDeptDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * iHR 部门信息规范化 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface OdsIhrDeptMapper extends BaseMapper<OdsIhrDeptDO> {

    /**
     * UPSERT：INSERT ON CONFLICT (ihr_dept_uuid) DO UPDATE SET ...
     *
     * @param o 规范化 DO（必须包含 ihrDeptUuid / rowHash / verNo / dqFlag）
     * @return 受影响行数
     */
    @Update("INSERT INTO ods_ihr_dept (id, ihr_dept_uuid, ihr_dept_id, department_code, dept_name, " +
            "parent_id, parent_department_code, dept_type, store_number, principal_staff_id, " +
            "dept_status, is_virtual, cost_name, cost_code, company_site_id, company_site_name, " +
            "sequence, row_hash, ver_no, dq_flag, created_at, updated_at, is_deleted) " +
            "VALUES (#{o.id}, #{o.ihrDeptUuid}, #{o.ihrDeptId}, #{o.departmentCode}, #{o.deptName}, " +
            "#{o.parentId}, #{o.parentDepartmentCode}, #{o.deptType}, #{o.storeNumber}, #{o.principalStaffId}, " +
            "#{o.deptStatus}, #{o.isVirtual}, #{o.costName}, #{o.costCode}, #{o.companySiteId}, #{o.companySiteName}, " +
            "#{o.sequence}, #{o.rowHash}, #{o.verNo}, #{o.dqFlag}, NOW(), NOW(), 0) " +
            "ON CONFLICT (ihr_dept_uuid) DO UPDATE SET " +
            "ihr_dept_id = EXCLUDED.ihr_dept_id, " +
            "department_code = EXCLUDED.department_code, " +
            "dept_name = EXCLUDED.dept_name, " +
            "parent_id = EXCLUDED.parent_id, " +
            "parent_department_code = EXCLUDED.parent_department_code, " +
            "dept_type = EXCLUDED.dept_type, " +
            "store_number = EXCLUDED.store_number, " +
            "principal_staff_id = EXCLUDED.principal_staff_id, " +
            "dept_status = EXCLUDED.dept_status, " +
            "is_virtual = EXCLUDED.is_virtual, " +
            "cost_name = EXCLUDED.cost_name, " +
            "cost_code = EXCLUDED.cost_code, " +
            "company_site_id = EXCLUDED.company_site_id, " +
            "company_site_name = EXCLUDED.company_site_name, " +
            "sequence = EXCLUDED.sequence, " +
            "row_hash = EXCLUDED.row_hash, " +
            "ver_no = EXCLUDED.ver_no, " +
            "dq_flag = EXCLUDED.dq_flag, " +
            "updated_at = NOW(), " +
            "is_deleted = 0")
    int upsert(@Param("o") OdsIhrDeptDO o);
}
