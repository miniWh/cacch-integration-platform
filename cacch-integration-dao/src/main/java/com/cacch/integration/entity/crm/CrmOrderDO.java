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
 * CRM 订单主表 DO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_crm_order", autoResultMap = true)
public class CrmOrderDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * CRM 订单内部 ID
     */
    private String crmOrderId;

    /**
     * CRM 订单编号（name），业务去重键
     */
    private String orderNo;

    private String customerId;

    private String customerName;

    private String ownerId;

    private String ownerName;

    private String currencyType;

    private String orderTotalAmount;

    private LocalDateTime crmCreateTime;

    private LocalDateTime crmModifyTime;

    /**
     * 明细拉取状态：PENDING / SUCCESS / FAILED
     */
    private String detailFetchStatus;

    private String detailFetchError;

    private LocalDateTime detailFetchTime;

    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object rawPayload;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
