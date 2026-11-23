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
 * Moka 待入职候选人 —— 教育经历 DO（EHR v2）
 *
 * <p>映射表 {@code t_integration_moka_entry_education}，与主表 1:N。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_entry_education")
public class MokaEntryEducationDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Integer applicationId;

    private String mokaEducationId;

    private Integer educationIndex;

    private String school;

    private String speciality;

    private String academicDegree;

    private String startDate;

    private String endDate;

    /**
     * 是否全日制：是 / 否
     */
    private String isFullTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}