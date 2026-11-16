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
 * iHR 岗位信息规范化 DO —— 映射 PG 表 {@code ods_ihr_position}
 *
 * <p>规范化层：字段清洗后的 iHR 岗位单条最新快照，
 * 业务主键为 {@code ihr_position_uuid}（UNIQUE 约束），
 * {@code rowHash} 用于行级幂等短路，{@code verNo} 每次内容变更递增。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("ods_ihr_position")
public class OdsIhrPositionDO {

    /**
     * 内部主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * iHR 职位 UUID（业务主键）
     */
    private String ihrPositionUuid;

    /**
     * 公司 ID（String UUID）
     */
    private String companyId;

    /**
     * 职位名称（iHR 最大长度100）
     */
    private String positionName;

    /**
     * 简称（iHR 最大长度100）
     */
    private String abbreviation;

    /**
     * 职位编号（iHR 最大长度50）
     */
    private String positionCode;

    /**
     * 应用范围：CURRENT_DEPARTMENT / CURRENT_DEPARTMENT_AND_CHILDREN / ALL_DEPARTMENT
     */
    private String appliedRange;

    /**
     * 当前编制人数
     */
    private Integer capacity;

    /**
     * 生效日期（iHR 返回 String，格式 YYYY-MM-DD）
     */
    private String effectiveDate;

    /**
     * 职位描述（iHR 最大长度255）
     */
    private String description;

    /**
     * 所属部门 ID（iHR 返回 Long，存字符串形式）
     */
    private String departmentId;

    /**
     * 所属部门名称（iHR 最大长度128）
     */
    private String departmentName;

    /**
     * 对应职务 ID（String UUID）
     */
    private String jobTitleId;

    /**
     * 对应职务名称
     */
    private String jobTitleName;

    /**
     * 对应职级 ID（String UUID）
     */
    private String positionGradeId;

    /**
     * 对应职级名称
     */
    private String positionGradeName;

    /**
     * 任职资格
     */
    private String qualifications;

    /**
     * 父级编号（String UUID；null 表示无上级）
     */
    private String parentId;

    /**
     * 是否为职位组：0-否 1-是
     */
    private Integer isPositionGroup;

    /**
     * 职位状态：ENABLE-启用 / DISABLE-停用
     */
    private String positionState;

    /**
     * 职位状态描述
     */
    private String positionStateString;

    /**
     * iHR 更新时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
     */
    private String updatedDate;

    /**
     * iHR 创建时间（原始 String，格式 YYYY-MM-DD HH:MM:SS）
     */
    private String createdDate;

    /**
     * 应用范围部门 ID 列表（JSONB 字符串，iHR 返回 List）
     */
    private String positionScope;

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
