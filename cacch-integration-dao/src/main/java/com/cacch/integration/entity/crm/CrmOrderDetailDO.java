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
 * CRM 订单明细 DO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_crm_order_detail", autoResultMap = true)
public class CrmOrderDetailDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联主表主键
     */
    private Long orderId;

    private String crmOrderId;

    private String orderNo;

    /**
     * CRM 明细 ID，去重键
     */
    private String crmDetailId;

    private String detailName;

    private String pdCode;

    private String pdCount;

    private String actualPrice;

    private String materialCode;

    /**
     * OA 同步状态
     */
    private String oaSyncStatus;

    private String oaProcessId;

    private LocalDateTime oaSyncTime;

    private Integer retryCount;

    private String lastErrorMsg;

    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object rawPayload;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
