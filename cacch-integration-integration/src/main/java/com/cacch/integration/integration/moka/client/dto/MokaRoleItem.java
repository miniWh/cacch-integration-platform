package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka 角色查询接口 —— 列表项
 *
 * <p>对应 Moka API {@code GET /api-platform/v1/users/roles?type=all}
 * 响应中 {@code data[*]} 的单条结构，字段完全对齐官方文档：</p>
 *
 * <pre>
 * {
 *   "id": 40,
 *   "name": "管理员",
 *   "role": 1,
 *   "description": "拥有系统全部权限"
 * }
 * </pre>
 *
 * <p>字段与 DB {@code t_integration_moka_role} 的映射关系：
 * id → role_id（业务主键）、name → role_name、role → role（整数角色值）、
 * description → description。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaRoleItem {

    /**
     * Moka 角色 ID（业务主键，对应 DB role_id）
     */
    private Integer id;

    /**
     * Moka 角色名称（对应 DB role_name）
     */
    private String name;

    /**
     * Moka 角色值（整数；对应 DB role 列）
     */
    private Integer role;

    /**
     * Moka 角色描述（可选，对应 DB description）
     */
    private String description;
}
