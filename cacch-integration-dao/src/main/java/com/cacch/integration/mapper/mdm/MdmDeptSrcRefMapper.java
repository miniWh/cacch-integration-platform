package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.MdmDeptSrcRefDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 部门主数据多源映射表 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MdmDeptSrcRefMapper extends BaseMapper<MdmDeptSrcRefDO> {

    /**
     * 按业务键查询 src_ref
     *
     * @param srcSysCode    来源系统编码（如 IHR）
     * @param srcEntityType 源系统实体类型（如 DEPT）
     * @param srcEntityId   源系统实体 ID（如 iHR 部门 uuid）
     * @return src_ref 记录；不存在时返回 null
     */
    @Select("SELECT id, dept_id, src_sys_code, src_entity_type, src_entity_id, created_at, updated_at, is_deleted "
            + "FROM mdm_dept_src_ref "
            + "WHERE src_sys_code = #{srcSysCode} "
            + "  AND src_entity_type = #{srcEntityType} "
            + "  AND src_entity_id = #{srcEntityId} "
            + "  AND is_deleted = 0")
    MdmDeptSrcRefDO selectByBusinessKey(@Param("srcSysCode") String srcSysCode,
                                        @Param("srcEntityType") String srcEntityType,
                                        @Param("srcEntityId") String srcEntityId);

    /**
     * 幂等插入
     *
     * @param id            内部主键
     * @param deptId        关联的 mdm_dept.id
     * @param srcSysCode    来源系统编码
     * @param srcEntityType 源系统实体类型
     * @param srcEntityId   源系统实体 ID
     */
    @Insert("INSERT INTO mdm_dept_src_ref (id, dept_id, src_sys_code, src_entity_type, src_entity_id) "
            + "VALUES (#{id}, #{deptId}, #{srcSysCode}, #{srcEntityType}, #{srcEntityId}) "
            + "ON CONFLICT (src_sys_code, src_entity_type, src_entity_id) DO NOTHING")
    int insertIgnore(@Param("id") Long id,
                     @Param("deptId") Long deptId,
                     @Param("srcSysCode") String srcSysCode,
                     @Param("srcEntityType") String srcEntityType,
                     @Param("srcEntityId") String srcEntityId);
}
