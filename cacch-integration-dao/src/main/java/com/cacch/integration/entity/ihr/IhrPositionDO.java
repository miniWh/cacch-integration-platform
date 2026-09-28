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
 * <p>主键为雪花 BIGINT（ASSIGN_ID），{@code uuid} 为 iHR 业务主键（UNIQUE 约束）。</p>
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
     * iHR 职位ID（业务主键，UNIQUE；文档声明 Long，存 String 兼容）
     */
    private String uuid;

    /**
     * 公司ID（文档声明 Long，存 VARCHAR 兼容）
     */
    private String companyId;

    /**
     * 职位名称
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
     * 部门ID（文档声明 Long，存 VARCHAR 兼容）
     */
    private String departmentId;

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
     * 上级职位ID（文档声明 Long，存 VARCHAR 兼容；null 表示无上级）
     */
    private String parentId;

    /**
     * 是否职位组
     */
    private Boolean isPositionGroup;

    /**
     * 职位状态：0-停用 1-启用
     */
    private Integer positionState;

    /**
     * 职位状态描述
     */
    private String positionStateString;

    /**
     * iHR 更新时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
     */
    private String updateDate;

    /**
     * iHR 创建时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
     */
    private String createDate;

    // ========== 审计/辅助字段 ==========

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
