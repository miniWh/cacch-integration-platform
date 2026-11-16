package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.integration.entity.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门主数据主档 DO —— 映射 PG 表 {@code mdm_dept}
 *
 * <p>标准公共字段（id/createdAt/updatedAt/isDeleted）继承自 {@link BaseDO}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_dept")
public class MdmDeptDO extends BaseDO {

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
}
