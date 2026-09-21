package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaRoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Moka 自定义角色中间表 Mapper
 *
 * <p>基础 CRUD 继承自 {@link BaseMapper}；业务唯一键为 {@code role_id}，
 * upsert 通过 PostgreSQL {@code INSERT ... ON CONFLICT (role_id) DO UPDATE} 原子语义实现。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaRoleMapper extends BaseMapper<MokaRoleDO> {

    /**
     * UPSERT：INSERT ON CONFLICT (role_id) DO UPDATE
     *
     * <p>以 {@code role_id} 为冲突检测键，已存在时覆盖 {@code role_name}、
     * {@code role} 和 {@code description}，保证与 Moka 侧最新数据对齐。</p>
     *
     * @param roleId      Moka 角色 ID（业务主键，对应 API id）
     * @param roleName    Moka 角色名称（对应 API name）
     * @param role        Moka 角色值（整数，对应 API role）
     * @param description Moka 角色描述（对应 API description）
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_moka_role (role_id, role_name, role, description) " +
            "VALUES (#{roleId}, #{roleName}, #{role}, #{description}) " +
            "ON CONFLICT (role_id) DO UPDATE SET " +
            "role_name = EXCLUDED.role_name, " +
            "role = EXCLUDED.role, " +
            "description = EXCLUDED.description")
    int upsert(@Param("roleId") Integer roleId,
               @Param("roleName") String roleName,
               @Param("role") Integer role,
               @Param("description") String description);
}
