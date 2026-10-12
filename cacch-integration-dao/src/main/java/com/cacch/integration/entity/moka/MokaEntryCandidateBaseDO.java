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
 * Moka 待入职候选人 —— 基础信息 DO（CandidateBaseInfo + ContactInfo）
 *
 * <p>映射 PG 表 {@code t_integration_moka_entry_candidate_base}。
 * 与主表 {@link MokaEntryApplicationDO} 为 1:1 关系，通过
 * {@code application_id} 关联。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_entry_candidate_base")
public class MokaEntryCandidateBaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联主表 application_id（1:1）
     */
    private Integer applicationId;

    // ========== CandidateBaseInfo ==========

    private String mokaName;
    private Integer mokaGender;
    private Integer mokaAge;
    private LocalDateTime mokaBirthDate;
    private String mokaNationality;
    private Integer mokaExperience;
    private LocalDateTime mokaCandidateCreatedAt;
    private Boolean mokaCandidateArchived;
    private LocalDateTime mokaCandidateArchivedAt;
    private Boolean mokaIsBlacklist;

    // ========== ContactInfo（1:1 合入） ==========

    /**
     * 手机号（敏感，日志脱敏）
     */
    private String mokaContactPhone;
    private String mokaContactEmail;
    private String mokaCountryCallingCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
