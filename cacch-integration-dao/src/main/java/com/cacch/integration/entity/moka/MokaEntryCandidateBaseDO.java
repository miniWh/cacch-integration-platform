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
 * Moka 待入职候选人 —— 基础信息 DO（CandidateBaseInfo + ContactInfo + EHR 登记表）
 *
 * <p>映射 PG 表 {@code t_integration_moka_entry_candidate_base}。
 * 与主表 {@link MokaEntryApplicationDO} 为 1:1 关系，通过 {@code application_id} 关联。</p>
 *
 * <p>数据来源：Step 1a {@code list_by_condition} 写入基础与联系方式；
 * Step 1d {@code listEhrApplicationsV2} 回写紧急联系人（面试登记表 customFields）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_entry_candidate_base")
public class MokaEntryCandidateBaseDO {

    /**
     * 内部主键（雪花算法，{@link IdType#ASSIGN_ID}）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Moka 申请 ID，关联主表 {@code application_id}（1:1 唯一）
     */
    private Integer applicationId;

    // ========== CandidateBaseInfo（Step 1a） ==========

    /**
     * 候选人姓名（Moka {@code candidateBaseInfo.name}）
     */
    private String mokaName;

    /**
     * 性别：1=男，2=女（Moka {@code genderCode}）
     */
    private Integer mokaGender;

    /**
     * 年龄（Moka {@code age}）
     */
    private Integer mokaAge;

    /**
     * 出生日期（Moka {@code birthDate}）
     */
    private LocalDateTime mokaBirthDate;

    /**
     * 国籍（Moka {@code nationality}）
     */
    private String mokaNationality;

    /**
     * 工作年限（Moka {@code experience}，单位：年）
     */
    private Integer mokaExperience;

    /**
     * 候选人在 Moka 侧的创建时间
     */
    private LocalDateTime mokaCandidateCreatedAt;

    /**
     * 候选人是否在 Moka 侧已归档
     */
    private Boolean mokaCandidateArchived;

    /**
     * 候选人在 Moka 侧的归档时间
     */
    private LocalDateTime mokaCandidateArchivedAt;

    /**
     * 是否黑名单候选人
     */
    private Boolean mokaIsBlacklist;

    // ========== ContactInfo（Step 1a，1:1 合入） ==========

    /**
     * 候选人手机号（敏感字段，日志须脱敏；Moka {@code contactInfo.phone}）
     */
    private String mokaContactPhone;

    /**
     * 候选人邮箱（Moka {@code contactInfo.email}）
     */
    private String mokaContactEmail;

    /**
     * 手机国际区号（Moka {@code contactInfo.countryCallingCode} / {@code prefixPhone}）
     */
    private String mokaCountryCallingCode;

    // ========== EHR v2 面试登记表（Step 1d，basicInfo.customFields） ==========

    /**
     * 紧急联系人姓名（Moka customFieldId {@code 399165}，section {@code basicInfo}，入库前 trim）
     */
    private String emergencyContactName;

    /**
     * 紧急联系人关系原文（Moka customFieldId {@code 412655}，如「父女」）
     */
    private String emergencyContactRelation;

    /**
     * 紧急联系人关系 iHR 下拉值（Q-02 映射结果，供二期「修改员工基础信息」自定义字段使用；未映射为 null）
     */
    private String emergencyContactRelationIhr;

    /**
     * 紧急联系人电话（Moka customFieldId {@code 410633}，敏感字段，日志须脱敏）
     */
    private String emergencyContactPhone;

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