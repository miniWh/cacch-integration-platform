package com.cacch.integration.entity.heart;

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
 * Heart 客户 PO 提交 CRM 订单记录 DO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_crm_heart_order_submit", autoResultMap = true)
public class HeartOrderSubmitDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单主表 ID
     */
    private Long poId;

    /**
     * 提交序号
     */
    private Integer attemptNo;

    /**
     * 提交结果：SUCCESS/FAILED
     */
    private String submitStatus;

    /**
     * CRM 订单数据 ID
     */
    private String crmContractId;

    /**
     * CRM 订单编号
     */
    private String crmContractNo;

    /**
     * CRM 取号流水号
     */
    private String crmSerialNo;

    private String returnCode;

    private String returnMsg;

    private Integer httpStatus;

    private String errorMessage;

    /**
     * 本次提交的完整请求报文
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object requestPayload;

    /**
     * 本次提交的完整响应报文
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object responsePayload;

    /**
     * 调用耗时（毫秒）
     */
    private Integer durationMs;

    /**
     * 链路追踪 TraceId
     */
    private String traceId;

    private LocalDateTime submittedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
