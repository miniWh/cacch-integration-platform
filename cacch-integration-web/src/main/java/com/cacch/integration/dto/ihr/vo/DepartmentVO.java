package com.cacch.integration.dto.ihr.vo;

import lombok.Data;

/**
 * IHR 部门视图
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class DepartmentVO {

    /**
     * 部门 UUID
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
     * 上级部门 ID
     */
    private String parentId;

    /**
     * 部门类型
     */
    private String type;

    /**
     * 部门编码
     */
    private String departmentCode;

    /**
     * 门店简称
     */
    private String shortNumber;

    /**
     * 部门负责人 staffId
     */
    private String principalStaffId;

    /**
     * 上级部门编码
     */
    private String parentDepartmentCode;

    /**
     * 上级部门名称
     */
    private String parentDepartmentName;

    /**
     * 是否虚拟节点
     */
    private Boolean virtual;

    /**
     * 部门状态
     */
    private String departmentStatus;

    /**
     * 组织描述
     */
    private String departmentDesc;

    /**
     * 组织属性
     */
    private String departmentProperty;

    /**
     * 最后更新时间
     */
    private String lastUpdate;

    /**
     * 创建时间（Unix 毫秒时间戳）
     */
    private Long createDate;

    /**
     * 部门简称
     */
    private String abbreviation;

    /**
     * 成立日期
     */
    private String establishDate;

    /**
     * 生效日期
     */
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
}
