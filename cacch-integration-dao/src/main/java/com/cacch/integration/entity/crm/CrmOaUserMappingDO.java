package com.cacch.integration.entity.crm;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.integration.dao.typehandler.PostgreSqlJsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CRM↔OA 人员映射 DO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_crm_oa_user_mapping", autoResultMap = true)
public class CrmOaUserMappingDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * CRM 员工 ID（订单 owner.id），业务唯一键
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
     * CRM 查询员工帐号完整响应 JSON
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object crmRawPayload;

    /**
     * OA 按编码取人员完整响应 JSON
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object oaRawPayload;

    /**
     * 最近一次成功映射时间
     */
    private LocalDateTime lastMappedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
