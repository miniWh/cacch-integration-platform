package com.cacch.integration.entity.ihr;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IHR 开放平台职位快照 DO —— 映射 PG 表 {@code t_integration_ihr_position}
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_ihr_position")
public class IhrPositionDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // ========== iHR 业务字段 ==========

    /**
     * 职位ID（String UUID，iHR 侧业务主键，UNIQUE）
     */
    private String uuid;

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
     * 生效日期（iHR 返回 String，格式 YYYY-MM-DD）
     */
    private String effectiveDate;

    /**
     * 职位描述（最大长度255）
     */
    private String description;

    /**
     * 所属部门id（Long 存 VARCHAR 兼容）
     */
    private String departmentId;

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
     * 父级编号（String UUID；null 表示无上级）
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
     * iHR 更新时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
     */
    private String updatedDate;

    /**
     * iHR 创建时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
     */
    private String createdDate;

    /**
     * 应用范围部门id列表（JSON 字符串，对应 PG JSONB 列；iHR 返回 List<Integer>）
     */
    private String positionScope;

    /**
     * 同步批次号（便于追溯每次同步写入的记录）
     */
    private String syncBatch;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
