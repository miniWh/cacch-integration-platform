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
 * iHR 部门信息规范化 DO —— 映射 PG 表 {@code ods_ihr_dept}
 *
 * <p>规范化层：字段清洗后的 iHR 部门单条最新快照，
 * 业务主键为 {@code ihr_dept_uuid}（UNIQUE 约束），
 * {@code rowHash} 用于行级幂等短路，{@code verNo} 每次内容变更递增。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("ods_ihr_dept")
public class OdsIhrDeptDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * iHR 部门 UUID
     */
    private String ihrDeptUuid;

    /**
     * iHR 部门 ID
     */
    private String ihrDeptId;

    /**
     * 部门编码
     */
    private String departmentCode;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 上级部门 ID
     */
    private String parentId;

    /**
     * 上级部门编码
     */
    private String parentDepartmentCode;

    /**
     * 部门类型：COMPANY / DIVISION / DEPARTMENT 等
     */
    private String deptType;

    /**
     * 门店编号
     */
    private String storeNumber;

    /**
     * 部门负责人 staffId
     */
    private String principalStaffId;

    /**
     * 部门状态：ENABLE 等
     */
    private String deptStatus;

    /**
     * 是否虚拟节点：0-否 1-是
     */
    private Integer isVirtual;

    /**
     * 排序号
     */
    private Integer sequence;

    /**
     * 变更检测哈希（SHA-256，未变则幂等短路）
     */
    private String rowHash;

    /**
     * 数据版本号（首次 1，每次内容变更 +1）
     */
    private Integer verNo;

    /**
     * 数据质量标记：0-正常 1-存在质量问题
     */
    private Integer dqFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
