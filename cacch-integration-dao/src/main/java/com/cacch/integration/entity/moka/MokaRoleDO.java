package com.cacch.integration.entity.moka;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Moka 自定义角色 DO —— 映射 PG 表 {@code t_integration_moka_role}
 *
 * <p>数据来源：Moka 开放平台角色查询接口
 * （{@code GET /api-platform/v1/users/roles?type=all}，对接文档 #-75）。
 * 接口 0 全量拉取后按 role_id upsert 落库，
 * 作为后续人员同步时 roleId 匹配 jobTitle 的依据。</p>
 *
 * <p>主键策略：雪花 BIGINT（{@link IdType#ASSIGN_ID}），
 * 业务唯一键 {@code role_id} 由 Moka 侧返回，单独建 UNIQUE 约束。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_role")
public class MokaRoleDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Moka 自定义角色 ID（业务唯一键，UNIQUE；对应 Moka API id）
     */
    private Integer roleId;

    /**
     * Moka 角色名称（对应 Moka API name）
     */
    private String roleName;

    /**
     * Moka 角色值（整数；对应 Moka API role）
     */
    private Integer role;

    /**
     * Moka 角色描述（对应 Moka API description）
     */
    private String description;

    /**
     * 记录创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-正常 1-删除
     */
    @TableLogic
    private Integer isDeleted;
}
