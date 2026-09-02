package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * IHR 部门实体（org/v1/organizations/search 响应 {@code data[]} 元素）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrDepartment {

    /**
     * 部门 UUID（IHR 内部唯一标识）
     */
    private String uuid;

    /**
     * 部门 ID
     */
    private String id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 上级部门 ID（无上级部门时该字段不返回）
     */
    @JsonProperty("parentId")
    private String parentId;

    /**
     * 部门类型（如 COMPANY / DIVISION / DEPARTMENT 等）
     */
    private String type;

    /**
     * 部门编码
     */
    @JsonProperty("departmentCode")
    private String departmentCode;

    /**
     * 门店简称
     */
    @JsonProperty("shortNumber")
    private String shortNumber;

    /**
     * 部门负责人 staffId
     */
    @JsonProperty("principalStaffId")
    private String principalStaffId;

    /**
     * 上级部门编码
     */
    @JsonProperty("parentDepartmentCode")
    private String parentDepartmentCode;

    /**
     * 上级部门名称
     */
    @JsonProperty("parentDepartmentName")
    private String parentDepartmentName;

    /**
     * 是否虚拟节点
     */
    private Boolean virtual;

    /**
     * 部门状态（ENABLED / DISABLED / DELETED 等）
     */
    @JsonProperty("departmentStatus")
    private String departmentStatus;

    /**
     * 组织描述
     */
    @JsonProperty("departmentDesc")
    private String departmentDesc;

    /**
     * 组织属性
     */
    @JsonProperty("departmentProperty")
    private String departmentProperty;

    /**
     * 最后更新时间（IHR 文档显示可为 null）
     */
    @JsonProperty("lastUpdate")
    private String lastUpdate;

    /**
     * 创建时间（IHR 返回 Unix 毫秒时间戳）
     */
    @JsonProperty("createDate")
    private Long createDate;

    /**
     * 部门简称
     */
    private String abbreviation;

    /**
     * 成立日期
     */
    @JsonProperty("establishDate")
    private String establishDate;

    /**
     * 生效日期
     */
    @JsonProperty("effectiveDate")
    private String effectiveDate;

    /**
     * 性质
     */
    private String nature;

    /**
     * 排序号
     */
    private Integer sequence;

    /**
     * 备注
     */
    private String remark;

    /**
     * 自定义字段 01..10（IHR 文档：对象值 1-2 级查询，不会被返回）
     */
    @JsonProperty("customField01")
    private String customField01;
    @JsonProperty("customField02")
    private String customField02;
    @JsonProperty("customField03")
    private String customField03;
    @JsonProperty("customField04")
    private String customField04;
    @JsonProperty("customField05")
    private String customField05;
    @JsonProperty("customField06")
    private String customField06;
    @JsonProperty("customField07")
    private String customField07;
    @JsonProperty("customField08")
    private String customField08;
    @JsonProperty("customField09")
    private String customField09;
    @JsonProperty("customField10")
    private String customField10;

    /**
     * 自定义字段的列表形式，保留扩展能力
     */
    @JsonProperty("customFields")
    private List<String> customFields;
}
