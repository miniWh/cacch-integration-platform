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
 * Moka 待入职候选人 —— 毕业院校 DO（GraduateSchool）
 *
 * <p>映射 PG 表 {@code t_integration_moka_entry_graduate_school}。
 * 与主表为 1:1 关系，通过 {@code application_id} 关联。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_entry_graduate_school")
public class MokaEntryGraduateSchoolDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联主表 application_id（1:1） */
    private Integer applicationId;

    /** 毕业专业 */
    private String lastSpeciality;

    /** 毕业学校 */
    private String lastSchool;

    /** 毕业时间（年份 Integer） */
    private Integer graduateDate;

    /** 学历/学位：1=其他 2=初中及以下 3=高中 4=中专 5=大专 6=本科 7=硕士 8=MBA 9=博士 */
    private Integer academicDegree;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
