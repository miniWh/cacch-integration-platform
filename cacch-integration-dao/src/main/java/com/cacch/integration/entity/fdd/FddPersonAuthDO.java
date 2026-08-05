package com.cacch.integration.entity.fdd;

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
 * 法大大个人实名认证记录实体，映射表 t_integration_fdd_person_auth
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_fdd_person_auth", autoResultMap = true)
public class FddPersonAuthDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 内部企业全称（业务判定键之一）
     */
    private String internalCompanyName;

    /**
     * 法大大侧认证流水号（回调匹配用）
     */
    private String transactionNo;

    /**
     * 姓名
     */
    private String personName;

    /**
     * 身份证号（业务判定键之一，明文）
     */
    private String idNumber;

    /**
     * 手机号（三要素业务记录，明文；法大大页面补充验证）
     */
    private String mobile;

    /**
     * 法大大本地用户唯一标识 accountId
     */
    private String fddAccountId;

    /**
     * 法大大认证页面 URL
     */
    private String authUrl;

    /**
     * 认证状态：PENDING / SUCCESS / FAILED
     */
    private String authStatus;

    /**
     * 发起认证时法大大请求/响应原始报文
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object requestDetail;

    /**
     * 法大大回调原始报文
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object authDetail;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 本次认证发起来源：CRM / OA（审计）
     */
    private String sourceSystem;

    /**
     * 来源系统业务单号
     */
    private String sourceBizNo;

    /**
     * 认证通过时间
     */
    private LocalDateTime certifiedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
