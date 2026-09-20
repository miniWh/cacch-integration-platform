package com.cacch.integration.entity.ihr;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IHR 开放平台组织架构部门快照 DO —— 映射 PG 表 {@code t_integration_ihr_department}
 *
 * <p>主键为雪花 BIGINT（ASSIGN_ID），{@code uuid} 为 iHR 业务主键（UNIQUE 约束）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_ihr_department")
public class IhrDepartmentDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // ========== iHR 业务字段 ==========

    /**
     * iHR 部门主键id（业务主键，UNIQUE）
     */
    private String uuid;

    /**
     * iHR 侧部门ID（存 VARCHAR 兼容极端情况；接口文档声明 Long）
     */
    private String ihrDeptId;

    /**
     * 部门名称（最大长度128）
     */
    private String name;

    /**
     * 上级部门ID（-1 表示无上级部门）
     */
    private String parentId;

    /**
     * 部门类型：COMPANY-公司 DEPARTMENT-部门 STORE-门店
     */
    private String type;

    /**
     * 部门编码
     */
    private String departmentCode;

    /**
     * 门店编号
     */
    private String storeNumber;

    /**
     * 部门负责人ID
     */
    private String principalStaffId;

    /**
     * 上级部门编码
     */
    private String parentDepartmentCode;

    /**
     * 上级部门名称
     */
    private String parentDepartmentName;

    /**
     * 是否虚拟节点
     */
    private Boolean virtual;

    /**
     * 部门状态：ENABLE-启用 DISABLE-停用
     */
    private String departmentStatus;

    /**
     * 组织描述
     */
    private String departmentDesc;

    /**
     * 组织属性（需从 iHR 选值接口获取选项值匹配）
     */
    @TableField("department_property")
    private String departmentProperty;

    /**
     * iHR 最后更新时间（原始 String）
     */
    private String lastUpdate;

    /**
     * iHR 创建时间（iHR 返回毫秒时间戳，同步时显式 Long→LocalDateTime 转换）
     */
    private LocalDateTime createdDate;

    /**
     * 简称
     */
    private String abbreviation;

    /**
     * 设立日期
     */
    private String establishDate;

    /**
     * 生效日期
     */
    private String effectiveDate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 顺序（iHR 返回 Integer）
     */
    private Integer sequence;

    // ========== 审计/辅助字段 ==========

    /**
     * 同步批次号（便于追溯每次同步写入的记录）
     */
    private String syncBatch;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
