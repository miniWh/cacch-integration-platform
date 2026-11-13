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
 * 部门主数据主档 DO —— 映射 PG 表 {@code mdm_dept}
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("mdm_dept")
public class MdmDeptDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 上级部门编码
     */
    private String parentDeptCode;

    /**
     * 部门类型：COMPANY / DIVISION / DEPARTMENT 等
     */
    private String deptType;

    /**
     * 部门状态：ENABLE 等
     */
    private String deptStatus;

    /**
     * 部门负责人 staffId
     */
    private String principalStaffId;

    /**
     * 排序号
     */
    private Integer sequence;

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
