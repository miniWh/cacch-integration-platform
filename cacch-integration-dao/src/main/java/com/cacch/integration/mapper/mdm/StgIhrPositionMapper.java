package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.StgIhrPositionDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * iHR 岗位信息贴源 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface StgIhrPositionMapper extends BaseMapper<StgIhrPositionDO> {

    /**
     * 幂等插入：INSERT ON CONFLICT (etl_batch_no, ihr_position_uuid) DO NOTHING
     *
     * <p>同批次同岗位的重复报文直接吞掉（返回 0 行），
     * 主键 id 由 Service 层预生成，冲突时自然弃用。</p>
     *
     * @param s 贴源 DO（必须包含 etlBatchNo / ihrPositionUuid / rawJson）
     * @return 受影响行数（冲突时 0）
     */
    @Insert("INSERT INTO stg_ihr_position (id, etl_batch_no, ihr_position_uuid, raw_json, http_status, created_at, updated_at, is_deleted) " +
            "VALUES (#{s.id}, #{s.etlBatchNo}, #{s.ihrPositionUuid}, CAST(#{s.rawJson} AS JSONB), #{s.httpStatus}, NOW(), NOW(), 0) " +
            "ON CONFLICT (etl_batch_no, ihr_position_uuid) DO NOTHING")
    int insertIgnore(@Param("s") StgIhrPositionDO s);
}
