package com.cacch.integration.dto.crm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * CRM↔OA 人员映射视图对象（对外返回，不含原始报文 JSON）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class CrmOaUserMappingVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * CRM 员工 ID（订单 creator_id.id），业务唯一键
     */
    private String crmEmployeeId;

    /**
     * CRM 员工登录帐号（queryEmployee.emp_code）
     */
    private String empCode;

    /**
     * OA 人员 ID（orgMembers.id）
     */
    private String oaUserId;

    /**
     * OA 登录名（供 Token loginName）
     */
    private String oaLoginName;

    /**
     * CRM 员工姓名（可选）
     */
    private String crmEmployeeName;

    /**
     * 最近一次成功映射时间
     */
    private LocalDateTime lastMappedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
