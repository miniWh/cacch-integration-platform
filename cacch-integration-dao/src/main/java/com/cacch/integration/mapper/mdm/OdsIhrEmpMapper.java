package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.OdsIhrEmpDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * iHR 员工信息规范化 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface OdsIhrEmpMapper extends BaseMapper<OdsIhrEmpDO> {

    /**
     * UPSERT：INSERT ON CONFLICT (ihr_staff_id) DO UPDATE SET ...
     *
     * @param o 规范化 DO（必须包含 ihrStaffId / rowHash / verNo / dqFlag）
     * @return 受影响行数
     */
    @Update("INSERT INTO ods_ihr_emp (id, ihr_staff_id, ihr_staff_no, ihr_staff_name, ihr_mobile, " +
            "ihr_email, ihr_work_email, ihr_id_card_type, ihr_id_card, ihr_dept_id, ihr_dept_name, " +
            "ihr_position_id, ihr_position_name, ihr_status, ihr_gender, ihr_education, " +
            "ihr_birthday, ihr_hire_date, ihr_leave_date, ihr_probation_end_date, " +
            "row_hash, ver_no, dq_flag, created_at, updated_at, is_deleted) " +
            "VALUES (#{o.id}, #{o.ihrStaffId}, #{o.ihrStaffNo}, #{o.ihrStaffName}, #{o.ihrMobile}, " +
            "#{o.ihrEmail}, #{o.ihrWorkEmail}, #{o.ihrIdCardType}, #{o.ihrIdCard}, #{o.ihrDeptId}, #{o.ihrDeptName}, " +
            "#{o.ihrPositionId}, #{o.ihrPositionName}, #{o.ihrStatus}, #{o.ihrGender}, #{o.ihrEducation}, " +
            "#{o.ihrBirthday}, #{o.ihrHireDate}, #{o.ihrLeaveDate}, #{o.ihrProbationEndDate}, " +
            "#{o.rowHash}, #{o.verNo}, #{o.dqFlag}, NOW(), NOW(), 0) " +
            "ON CONFLICT (ihr_staff_id) DO UPDATE SET " +
            "ihr_staff_no = EXCLUDED.ihr_staff_no, " +
            "ihr_staff_name = EXCLUDED.ihr_staff_name, " +
            "ihr_mobile = EXCLUDED.ihr_mobile, " +
            "ihr_email = EXCLUDED.ihr_email, " +
            "ihr_work_email = EXCLUDED.ihr_work_email, " +
            "ihr_id_card_type = EXCLUDED.ihr_id_card_type, " +
            "ihr_id_card = EXCLUDED.ihr_id_card, " +
            "ihr_dept_id = EXCLUDED.ihr_dept_id, " +
            "ihr_dept_name = EXCLUDED.ihr_dept_name, " +
            "ihr_position_id = EXCLUDED.ihr_position_id, " +
            "ihr_position_name = EXCLUDED.ihr_position_name, " +
            "ihr_status = EXCLUDED.ihr_status, " +
            "ihr_gender = EXCLUDED.ihr_gender, " +
            "ihr_education = EXCLUDED.ihr_education, " +
            "ihr_birthday = EXCLUDED.ihr_birthday, " +
            "ihr_hire_date = EXCLUDED.ihr_hire_date, " +
            "ihr_leave_date = EXCLUDED.ihr_leave_date, " +
            "ihr_probation_end_date = EXCLUDED.ihr_probation_end_date, " +
            "row_hash = EXCLUDED.row_hash, " +
            "ver_no = EXCLUDED.ver_no, " +
            "dq_flag = EXCLUDED.dq_flag, " +
            "updated_at = NOW(), " +
            "is_deleted = 0")
    int upsert(@Param("o") OdsIhrEmpDO o);
}
