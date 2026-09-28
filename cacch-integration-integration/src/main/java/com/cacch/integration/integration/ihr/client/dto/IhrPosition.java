package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;

import java.util.List;

/**
 * IHR 获取公司职位清单 — 职位项响应 DTO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrPosition {

    /**
     * 职位ID（String UUID，iHR 业务主键）
     */
    private String id;

    /**
     * 公司ID（String UUID）
     */
    private String companyId;

    /**
     * 职位名称（最大长度100）
     */
    private String positionName;

    /**
     * 简称（最大长度100）
     */
    private String abbreviation;

    /**
     * 职位编号（最大长度50）
     */
    private String positionCode;

    /**
     * 应用范围：CURRENT_DEPARTMENT / CURRENT_DEPARTMENT_AND_CHILDREN / ALL_DEPARTMENT
     */
    private String appliedRange;

    /**
     * 当前编制人数
     */
    private Integer capacity;

    /**
     * 生效日期（iHR 返回 String）
     */
    private String effectiveDate;

    /**
     * 所属部门id（Long）
     */
    private Long departmentId;

    /**
     * 职位描述（最大长度255）
     */
    private String description;

    /**
     * 所属部门名称（最大长度128）
     */
    private String departmentName;

    /**
     * 对应职务id（String UUID）
     */
    private String jobTitleId;

    /**
     * 对应职务名称
     */
    private String jobTitleName;

    /**
     * 对应职级id（String UUID）
     */
    private String positionGradeId;

    /**
     * 对应职级名称
     */
    private String positionGradeName;

    /**
     * 任职资格
     */
    private String qualifications;

    /**
     * 父级编号（String UUID，null 表示无上级）
     */
    private String parentId;

    /**
     * 是否为职位组
     */
    private Boolean isPositionGroup;

    /**
     * 职位状态：ENABLE / DISABLE
     */
    private String positionState;

    /**
     * 职位状态描述
     */
    private String positionStateString;

    /**
     * 更新时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
     */
    private String updatedDate;

    /**
     * 创建时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
     */
    private String createdDate;

    /**
     * 应用范围部门id列表（positionScope 是部门 ID 数组；存 List，写入 DB 时序列化为 JSONB）
     */
    private List<Integer> positionScope;
}
