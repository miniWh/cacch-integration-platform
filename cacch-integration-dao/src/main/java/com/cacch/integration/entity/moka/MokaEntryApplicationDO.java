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
 * Moka 待入职候选人 —— 申请主表 DO
 *
 * <p>映射 PG 表 {@code t_integration_moka_entry_application}。
 * 数据来源链路：Moka {@code list_by_condition} → 本主表 + 3 子表 + 状态表。
 * 补全字段（department_code / check_in_date / offer_department_code）
 * 在 Step 1b（职位详情接口）和 Step 1c（Offer 扩展接口）回写。</p>
 *
 * <p>主键策略：雪花 BIGINT（{@link IdType#ASSIGN_ID}），
 * 业务唯一键 {@code application_id} 单独建 UNIQUE 约束。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_moka_entry_application")
public class MokaEntryApplicationDO {

    /**
     * 内部主键（雪花生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Moka 申请 ID（业务唯一键，UNIQUE）
     */
    private Integer applicationId;

    /**
     * Moka 候选人 ID
     */
    private Integer candidateId;

    /**
     * 招聘模式：1=社招 2=校招
     */
    private Integer hireMode;

    /**
     * 申请时间
     */
    private LocalDateTime appliedAt;

    /**
     * 移动阶段时间
     */
    private LocalDateTime movedAt;

    /**
     * Moka 侧创建时间
     */
    private LocalDateTime mokaCreatedAt;

    /**
     * Moka 侧更新时间
     */
    private LocalDateTime mokaUpdatedAt;

    // ========== Job ==========

    /**
     * Moka 职位 ID（Step 1b 补全部门编码的查询键）
     */
    private String mokaJobId;

    /**
     * Moka 职位名称
     */
    private String mokaJobTitle;

    /**
     * Moka 职位原始部门名（list_by_condition 返回）
     */
    private String mokaJobDepartment;

    // ========== Pipeline ==========

    /**
     * Moka 流程 ID
     */
    private Integer mokaPipelineId;

    /**
     * Moka 流程名称
     */
    private String mokaPipelineName;

    // ========== Stage ==========

    /**
     * Moka 阶段 ID
     */
    private Integer mokaStageId;

    /**
     * Moka 阶段名称
     */
    private String mokaStageName;

    /**
     * Moka 阶段类型：102=待入职
     */
    private Integer mokaStageType;

    // ========== OwnerInfo ==========

    /**
     * 申请所有者 ID
     */
    private Integer mokaOwnerId;

    /**
     * 申请所有者姓名
     */
    private String mokaOwnerName;

    // ========== 补全字段（Step 1b/1c 回写） ==========

    /**
     * 【Step 1b 补全】职位所属部门编码（code），第二步匹配 iHR 部门的关键字段
     */
    private String mokaDepartmentCode;

    /**
     * 【Step 1c 补全】入职日期（checkinDate），iHR expectEntryDate 来源
     */
    private LocalDateTime checkInDate;

    /**
     * 【Step 1c 补全】Offer 入职部门编码（备选，通常比职位部门更准确）
     */
    private String offerDepartmentCode;

    /**
     * 【Step 1c 补全】Offer 入职部门名称
     */
    private String offerDepartmentName;

    /**
     * 【Step 1c 补全】Offer 当前状态：pending/accepted/rejected/invalid/expired
     */
    private String offerStatus;

    // ========== ArchiveReasons（待入职通常为 null） ==========

    private Integer archiveReasonId;
    private Integer archiveReasonType;
    private String archiveReasonName;
    private LocalDateTime archivedAt;

    // ========== 审计/辅助 ==========

    /**
     * 同步批次号（便于追溯）
     */
    private String syncBatch;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
