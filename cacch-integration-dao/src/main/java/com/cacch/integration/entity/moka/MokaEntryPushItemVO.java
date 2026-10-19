package com.cacch.integration.entity.moka;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Moka 待入职推送 iHR —— 查询结果 VO
 *
 * <p>承载 4 表 JOIN（主表 + candidate_base + citizen + graduate_school）+
 * iHR 部门快照的联合查询结果，用于推送前的数据组装。
 * 包含 iHR 推送所需的全部字段（含通过 department_code JOIN 得到的 ihr_dept_id）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaEntryPushItemVO {

    /**
     * Moka 申请 ID（业务唯一键，关联同步状态表）
     */
    private Integer applicationId;

    // ========== 候选人基础信息 ==========

    /**
     * 候选人姓名
     */
    private String name;

    /**
     * 性别：1-男 2-女
     */
    private Integer gender;

    /**
     * 手机号（iHR 必填）
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 国籍
     */
    private String nationality;

    // ========== 证件信息（Step 1a 入库为 null，待排查） ==========

    /**
     * 证件号（iHR 可空）
     */
    private String citizenId;

    /**
     * 证件类型：1-身份证 2-香港身份证 3-澳门身份证 4-台湾身份证 5-护照 6-其他
     */
    private Integer certificateType;

    // ========== 职位/Offer 信息 ==========

    /**
     * Moka 职位部门编码（Step 1b 补全）
     */
    private String mokaDepartmentCode;

    /**
     * Offer 入职部门名称
     */
    private String offerDepartmentName;

    /**
     * Offer 入职部门编码
     */
    private String offerDepartmentCode;

    /**
     * 入职日期（Step 1c 补全，iHR 必填）
     */
    private LocalDateTime checkInDate;

    /**
     * Offer 状态：pending/accepted/rejected/invalid/expired
     */
    private String offerStatus;

    // ========== 招聘元信息 ==========

    /**
     * 招聘模式：1-社招 2-校招
     */
    private Integer hireMode;

    /**
     * Moka 职位名称
     */
    private String mokaJobTitle;

    /**
     * 招聘阶段名称
     */
    private String mokaStageName;

    /**
     * 招聘负责人
     */
    private String mokaOwnerName;

    // ========== 学历/毕业信息 ==========

    /**
     * 毕业院校
     */
    private String lastSchool;

    /**
     * 所学专业
     */
    private String lastSpeciality;

    /**
     * 学历：1-其他 2-初中及以下 3-高中 4-中专 5-大专 6-本科 7-硕士 8-MBA 9-博士
     */
    private Integer academicDegree;

    /**
     * 毕业时间（毫秒时间戳）
     */
    private Long graduateDate;

    // ========== iHR 关联信息 ==========

    /**
     * iHR 部门数字 ID（通过 department_code JOIN 得到，iHR 必填）
     */
    private Long ihrDeptId;

    /**
     * iHR 父级部门 ID
     */
    private String ihrParentId;

    // ========== 同步状态 ==========

    /**
     * iHR 同步状态：PENDING / SUCCESS / FAILED / SKIPPED
     */
    private String ihrSyncStatus;

    /**
     * iHR 返回的 entryId（推送成功时回填）
     */
    private String ihrEntryId;
}
