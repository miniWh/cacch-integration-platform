package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaDepartmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Moka 组织架构部门主表 Mapper
 *
 * <p>主键为业务字段 department_code（非雪花 BIGINT），
 * 使用 {@link MokaDepartmentDO#getDepartmentCode()} 作为 id 查询条件。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaDepartmentMapper extends BaseMapper<MokaDepartmentDO> {

    /**
     * 按父部门编码查询直接子部门列表
     *
     * @param parentCode 父部门编码，一级部门传 "0"
     * @return 子部门列表；父部门不存在时返回空列表
     */
    @Select("SELECT * FROM t_integration_moka_department WHERE parent_code = #{parentCode} ORDER BY sequence, create_time")
    List<MokaDepartmentDO> selectByParentCode(@Param("parentCode") String parentCode);

    /**
     * 按类型筛选部门列表
     *
     * @param type 部门类型：1-普通部门，2-门店部门
     * @return 匹配类型的部门列表
     */
    @Select("SELECT * FROM t_integration_moka_department WHERE type = #{type} ORDER BY sequence, create_time")
    List<MokaDepartmentDO> selectByType(@Param("type") Integer type);

    /**
     * 按 Moka 同步状态筛选部门列表
     *
     * @param mokaSyncStatus 同步状态：0-未同步 1-已同步 2-同步失败
     * @return 匹配状态的部门列表
     */
    @Select("SELECT * FROM t_integration_moka_department WHERE moka_sync_status = #{mokaSyncStatus} ORDER BY sequence, create_time")
    List<MokaDepartmentDO> selectByMokaSyncStatus(@Param("mokaSyncStatus") Integer mokaSyncStatus);

    /**
     * UPSERT：INSERT ON CONFLICT (department_code) DO UPDATE
     *
     * <p>PostgreSQL 12 兼容写法，使用 {@code EXCLUDED} 虚拟表引用待插入值。</p>
     *
     * @param departmentCode 部门编码（主键）
     * @param name           部门名称
     * @param parentCode     父部门编码
     * @param type           部门类型
     * @param sequence       排序号（整数）
     * @param mokaSyncStatus Moka 同步状态
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_moka_department (department_code, name, parent_code, type, sequence, moka_sync_status) " +
            "VALUES (#{departmentCode}, #{name}, #{parentCode}, #{type}, #{sequence}, #{mokaSyncStatus}) " +
            "ON CONFLICT (department_code) DO UPDATE SET " +
            "name = EXCLUDED.name, " +
            "parent_code = EXCLUDED.parent_code, " +
            "type = EXCLUDED.type, " +
            "sequence = EXCLUDED.sequence, " +
            "moka_sync_status = EXCLUDED.moka_sync_status")
    int upsert(@Param("departmentCode") String departmentCode,
               @Param("name") String name,
               @Param("parentCode") String parentCode,
               @Param("type") Integer type,
               @Param("sequence") Integer sequence,
               @Param("mokaSyncStatus") Integer mokaSyncStatus);

    /**
     * 更新指定部门的 Moka 同步状态
     *
     * @param departmentCode 部门编码
     * @param mokaSyncStatus 目标状态：0-未同步 1-已同步 2-同步失败
     * @return 受影响行数
     */
    @Update("UPDATE t_integration_moka_department SET moka_sync_status = #{mokaSyncStatus} WHERE department_code = #{departmentCode}")
    int updateMokaSyncStatus(@Param("departmentCode") String departmentCode,
                             @Param("mokaSyncStatus") Integer mokaSyncStatus);
}
