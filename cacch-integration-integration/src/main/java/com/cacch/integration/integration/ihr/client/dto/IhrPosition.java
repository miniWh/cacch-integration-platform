package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * IHR 获取公司职位清单 — 职位项响应 DTO
 *
 * <p>字段映射严格对齐 iHR 接口文档：GET /api/v1/org/{orgId}/positions
 * 注意：文档中职位接口成功 {@code code=200}（部门清单 v3 为 {@code code=0}）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrPosition {

    /**
     * 职位ID（iHR 业务主键，UNIQUE；文档声明 Long）
     */
    private Long id;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 职位名称（最大长度128）
     */
    private String positionName;

    /**
     * 简称
     */
    private String abbreviation;

    /**
     * 职位编码
     */
    private String positionCode;

    /**
     * 适用范围（如 CURRENT_DEPARTMENT）
     */
    private String applyRange;

    /**
     * 编制人数
     */
    private Integer capacity;

    /**
     * 生效日期（iHR 返回 String，格式 YYYY-MM-DD）
     */
    private String affectiveDate;

    /**
     * 失效日期（iHR 返回 String，格式 YYYY-MM-DD）
     */
    private String expiryDate;

    /**
     * 职位范围
     */
    private String positionScope;

    /**
     * 职位描述
     */
    private String description;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 职能
     */
    private String jobFunction;

    /**
     * 子职能
     */
    private String jobSubFunction;

    /**
     * 是否已定级
     */
    private Boolean positionGraded;

    /**
     * 职级名称
     */
    private String positionGradeName;

    /**
     * 任职资格
     */
    private String qualification;

    /**
     * 上级职位ID（null 表示无上级）
     */
    private Long parentId;

    /**
     * 是否职位组
     */
    private Boolean isPositionGroup;

    /**
     * 职位状态：0-停用 1-启用
     */
    private Integer positionState;

    /**
     * 职位状态描述（如 "启用" / "停用"）
     */
    private String positionStateString;

    /**
     * iHR 更新时间（格式 YYYY-MM-DD HH:MM:SS）
     */
    private String updateDate;

    /**
     * iHR 创建时间（格式 YYYY-MM-DD HH:MM:SS）
     */
    private String createDate;
}
