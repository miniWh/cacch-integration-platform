package com.cacch.integration.service.moka.api;

import com.cacch.integration.entity.moka.MokaRoleDO;

import java.util.List;

/**
 * Moka 自定义角色持久化服务接口
 *
 * <p>职责边界：单聚合内的 DB 读写，负责按业务主键 role_id upsert、
 * 查询、清理等；不直接调用任何第三方 HTTP Client。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaRoleService {

    /**
     * 按 role_id 查询角色
     *
     * @param roleId Moka 自定义角色 ID（业务主键）
     * @return 角色实体；不存在时返回 null
     */
    MokaRoleDO getByRoleId(Integer roleId);

    /**
     * 按 role_name 查询角色
     *
     * @param roleName 角色名称
     * @return 角色实体；不存在时返回 null
     */
    MokaRoleDO getByRoleName(String roleName);

    /**
     * 查询全部未逻辑删除的角色
     *
     * @return 角色列表；无数据时返回空列表（非 null）
     */
    List<MokaRoleDO> listAll();

    /**
     * 单条 upsert —— 以 role_id 为业务主键，存在则更新 role_name / description
     *
     * @param role 待写入的角色实体（role_id 必填）
     * @return upsert 后从 DB 重新查询的实体
     */
    MokaRoleDO upsert(MokaRoleDO role);

    /**
     * 批量 upsert —— 逐条 try-catch，单条失败不阻断其余，汇总成功/失败数
     *
     * @param roles 待写入的角色实体列表；null 或空时直接返回 0
     * @return 成功 upsert 的条数
     */
    int batchUpsert(List<MokaRoleDO> roles);

    /**
     * 清空全部角色（用于全量覆盖同步前先清库）
     *
     * @return 实际删除的行数
     */
    int deleteAll();
}
