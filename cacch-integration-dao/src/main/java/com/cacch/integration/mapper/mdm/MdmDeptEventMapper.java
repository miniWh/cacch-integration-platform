package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.MdmDeptEventDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 部门主数据变更事件 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MdmDeptEventMapper extends BaseMapper<MdmDeptEventDO> {

    /**
     * 幂等插入：INSERT ON CONFLICT (event_id) DO NOTHING
     *
     * <p>同一事件（deptUuid + eventType + rowHash 决定）重复产生时
     * 直接吞掉（返回 0 行）；主键 id 由 Service 层预生成（雪花）。</p>
     *
     * @param e 事件 DO（必须包含 eventId / eventType / changeLevel / deptId）
     * @return 受影响行数（冲突时 0）
     */
    @Insert("INSERT INTO mdm_dept_event (id, event_id, event_type, change_level, dept_id, dept_code, payload, etl_batch_no, status, created_at, updated_at, is_deleted) " +
            "VALUES (#{e.id}, #{e.eventId}, #{e.eventType}, #{e.changeLevel}, #{e.deptId}, #{e.deptCode}, CAST(#{e.payload} AS JSONB), #{e.etlBatchNo}, #{e.status}, NOW(), NOW(), 0) " +
            "ON CONFLICT (event_id) DO NOTHING")
    int insertIgnore(@Param("e") MdmDeptEventDO e);
}
