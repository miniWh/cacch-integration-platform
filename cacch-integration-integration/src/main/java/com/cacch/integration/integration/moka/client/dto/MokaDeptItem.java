package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Moka 组织架构 —— 单条部门数据（获取全量组织架构响应项）
 *
 * <p>字段匹配 Moka API {@code GET /api-platform/v1/departments} 的 data 数组元素。
 * 相比写入方向 {@link MokaDepartment}，响应项额外包含系统字段：
 * id / parentId / status / isDeleted / createdTime / lastModifiedTime。</p>
 *
 * <p><strong>字段说明</strong>（基于 Moka 文档推断，实际字段以 Moka 响应为准）：
 * <ul>
 *     <li>{@code id}：Moka 系统内部门自增 ID</li>
 *     <li>{@code departmentCode}：客户系统的部门主键（与写入接口一致）</li>
 *     <li>{@code parentCode}：上级部门 departmentCode；一级部门为 "0"</li>
 *     <li>{@code parentId}：Moka 系统内上级部门自增 ID</li>
 *     <li>{@code status}：部门状态：1 正常 / 0 停用</li>
 *     <li>{@code isDeleted}：逻辑删除标记：0 正常 / 1 已删除</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptItem {

    /**
     * Moka 系统内部门自增 ID
     */
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 客户系统的部门 id（主键）
     */
    private String departmentCode;

    /**
     * 上级部门的 departmentCode；一级部门为 "0"
     */
    private String parentCode;

    /**
     * Moka 系统内上级部门自增 ID；一级部门为 0
     */
    private Long parentId;

    /**
     * 部门类型：1 普通部门 / 2 门店部门
     */
    private Integer type;

    /**
     * 部门排序，0~10000
     */
    private Integer sequence;

    /**
     * 部门状态：1 正常 / 0 停用
     */
    private Integer status;

    /**
     * 逻辑删除标记：0 正常 / 1 已删除
     */
    @JsonProperty("isDeleted")
    private Integer isDeleted;

    /**
     * 创建时间，格式 yyyy-MM-dd HH:mm:ss
     */
    private String createdTime;

    /**
     * 最后修改时间，格式 yyyy-MM-dd HH:mm:ss
     */
    private String lastModifiedTime;

    /**
     * 部门名称的多语言信息
     */
    private List<MokaDepartment.MokaLocalizedName> localizedNames;
}
