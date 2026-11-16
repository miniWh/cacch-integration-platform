package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位主数据主档 DO —— 映射 PG 表 {@code mdm_position}
 *
 * <p>主档本体不存来源标识（iHR 职位 uuid 经 {@code mdm_position_src_ref} 映射），
 * 岗位定位与多源追溯经 src_ref 进行。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("mdm_position")
public class MdmPositionDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 职位编号（iHR positionCode，覆盖更新）
     */
    private String positionCode;

    /**
     * 职位名称
     */
    private String positionName;

    /**
     * 父级职位编号（iHR parentId，UUID；允许乱序建档）
     */
    private String parentPositionId;

    /**
     * 所属部门 ID（暂存 iHR departmentId 字符串形式，后续融合 md_org）
     */
    private String departmentId;

    /**
     * 所属部门名称
     */
    private String departmentName;

    /**
     * 对应职务 ID
     */
    private String jobTitleId;

    /**
     * 对应职务名称
     */
    private String jobTitleName;

    /**
     * 对应职级 ID
     */
    private String positionGradeId;

    /**
     * 对应职级名称
     */
    private String positionGradeName;

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
     * 职位状态：ENABLE-启用 / DISABLE-停用
     */
    private String positionState;

    /**
     * 首次建档来源系统：IHR
     */
    private String srcSysCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
