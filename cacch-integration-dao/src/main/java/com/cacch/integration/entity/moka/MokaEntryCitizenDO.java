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
 * Moka 待入职候选人 —— 证件信息 DO（Citizen）
 *
 * <p>映射 PG 表 {@code t_integration_moka_entry_citizen}。
 * 与主表为 1:1 关系，通过 {@code application_id} 关联。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_entry_citizen")
public class MokaEntryCitizenDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联主表 application_id（1:1） */
    private Integer applicationId;

    /** 证件号码（敏感，日志脱敏） */
    private String citizenId;

    /** 证件类型：1=身份证 2=香港身份证 3=澳门身份证 4=台湾身份证 5=护照 6=其他证件 */
    private Integer certificateType;

    /** 签发机关 */
    private String idCardSignOrganization;

    /** 证件有效期（原始 JSON 数组存 VARCHAR） */
    private String idCardValidity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
