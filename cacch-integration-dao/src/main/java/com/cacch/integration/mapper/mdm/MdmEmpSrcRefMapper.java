package com.cacch.integration.mapper.mdm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.mdm.MdmEmpSrcRefDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 人员主数据多源映射表 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MdmEmpSrcRefMapper extends BaseMapper<MdmEmpSrcRefDO> {

    /**
     * 按业务键查询 src_ref
     *
     * @param srcSysCode    来源系统编码（如 IHR）
     * @param srcEntityType 源系统实体类型（如 STAFF）
     * @param srcEntityId   源系统实体 ID（如 iHR staffId UUID）
     * @return src_ref 记录；不存在时返回 null
     */
    @Select("SELECT id, md_code, src_sys_code, src_entity_type, src_entity_id, created_at, updated_at, is_deleted "
            + "FROM mdm_emp_src_ref "
            + "WHERE src_sys_code = #{srcSysCode} "
            + "  AND src_entity_type = #{srcEntityType} "
            + "  AND src_entity_id = #{srcEntityId} "
            + "  AND is_deleted = 0")
    MdmEmpSrcRefDO selectByBusinessKey(@Param("srcSysCode") String srcSysCode,
                                       @Param("srcEntityType") String srcEntityType,
                                       @Param("srcEntityId") String srcEntityId);

    /**
     * 幂等插入
     *
     * @param id            内部主键（雪花生成，由调用方填充）
     * @param mdCode        关联的 mdm_emp.md_code
     * @param srcSysCode    来源系统编码
     * @param srcEntityType 源系统实体类型
     * @param srcEntityId   源系统实体 ID
     */
    @Insert("INSERT INTO mdm_emp_src_ref (id, md_code, src_sys_code, src_entity_type, src_entity_id) "
            + "VALUES (#{id}, #{mdCode}, #{srcSysCode}, #{srcEntityType}, #{srcEntityId}) "
            + "ON CONFLICT (src_sys_code, src_entity_type, src_entity_id) DO NOTHING")
    int insertIgnore(@Param("id") Long id,
                     @Param("mdCode") Long mdCode,
                     @Param("srcSysCode") String srcSysCode,
                     @Param("srcEntityType") String srcEntityType,
                     @Param("srcEntityId") String srcEntityId);
}
