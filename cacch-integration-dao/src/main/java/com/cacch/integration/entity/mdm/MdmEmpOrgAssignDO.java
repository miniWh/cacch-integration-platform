package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人员任职记录 DO —— 映射 PG 表 {@code mdm_emp_org_assign}（SCD2 拉链）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("mdm_emp_org_assign")
public class MdmEmpOrgAssignDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联 mdm_emp.id
     */
    private Long empId;

    /**
     * 组织编码
     */
    private String orgCode;

    /**
     * 组织名称（iHR departmentName）
     */
    private String orgName;

    /**
     * 职位编码（iHR positionId）
     */
    private String positionCode;

    /**
     * 职位名称（iHR positionName）
     */
    private String positionName;

    /**
     * 任职状态：PENDING_ONBOARD-待入职 / PROBATION-试用 / ACTIVE-正式 / ENDED-已结束
     */
    private String assignStatus;

    /**
     * 主任职标记：Y-是（当前单任职来源，恒 Y）
     */
    private String principalFlag;

    /**
     * 生效起始日（入职日期）
     */
    private LocalDate validFrom;

    /**
     * 生效截止日（NULL 表示当前有效）
     */
    private LocalDate validTo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
