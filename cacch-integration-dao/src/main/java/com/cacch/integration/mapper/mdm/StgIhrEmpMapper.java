package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.StgIhrEmpDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * iHR 员工信息贴源 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface StgIhrEmpMapper extends BaseMapper<StgIhrEmpDO> {

    /**
     * 幂等插入：INSERT ON CONFLICT (etl_batch_no, ihr_staff_id) DO NOTHING
     *
     * <p>同批次同员工的重复报文直接吞掉（返回 0 行），
     * 主键 id 由 Service 层预生成（雪花），冲突时自然弃用。</p>
     *
     * @param s 贴源 DO（必须包含 etlBatchNo / ihrStaffId / rawJson）
     */
    @Insert("INSERT INTO stg_ihr_emp (id, etl_batch_no, ihr_staff_id, raw_json, http_status, created_at, updated_at, is_deleted) " +
            "VALUES (#{s.id}, #{s.etlBatchNo}, #{s.ihrStaffId}, CAST(#{s.rawJson} AS JSONB), #{s.httpStatus}, NOW(), NOW(), 0) " +
            "ON CONFLICT (etl_batch_no, ihr_staff_id) DO NOTHING")
    void insertIgnore(@Param("s") StgIhrEmpDO s);
}
