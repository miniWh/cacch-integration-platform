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
 * <p>映射表 {@code t_integration_moka_entry_education}，与主表 {@link MokaEntryApplicationDO}
 * 为 1:N 关系（同一 {@code application_id} 可有多条教育记录）。</p>
 *
 * <p>数据来源：Step 1d {@code listEhrApplicationsV2} 的 {@code basicInfo.educationInfo}，
 * 「是否全日制」来自同 section 下 customFieldId {@code 399166}（按 {@code builtinInfoId} 或
 * {@code index} 与教育行对齐）。Step 1d 按申请全量替换本表数据。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_entry_education")
public class MokaEntryEducationDO {

    /**
     * 内部主键（雪花算法，{@link IdType#ASSIGN_ID}）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Moka 申请 ID，关联主表 {@code application_id}
     */
    private Integer applicationId;

    /**
     * Moka 教育经历行 ID（{@code educationInfo[].id}，与 customFields.builtinInfoId 对齐）
     */
    private String mokaEducationId;

    /**
     * 教育经历行序号（从 0 起，与 Moka customFields.index / educationInfo 数组下标对齐）
     */
    private Integer educationIndex;

    /**
     * 学校名称（Moka {@code educationInfo.school}）
     */
    private String school;

    /**
     * 所学专业（Moka {@code educationInfo.speciality}）
     */
    private String speciality;

    /**
     * 学历/学位文本（Moka {@code educationInfo.academicDegree}，如「硕士」「本科」）
     */
    private String academicDegree;

    /**
     * 入校时间（Moka {@code educationInfo.startDate}，格式通常为 yyyy-MM）
     */
    private String startDate;

    /**
     * 结业/毕业时间（Moka {@code educationInfo.endDate}，格式通常为 yyyy-MM）
     */
    private String endDate;

    /**
     * 是否全日制：{@code 是} / {@code 否}；无 Moka 字段匹配时为 null（customFieldId 399166，bool {@code true}/{@code false} 转换）
     */
    private String isFullTime;

    /**
     * 本行记录创建时间（平台入库时间）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 本行记录最近更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0=正常，1=已删除
     */
    @TableLogic
    private Integer isDeleted;
}