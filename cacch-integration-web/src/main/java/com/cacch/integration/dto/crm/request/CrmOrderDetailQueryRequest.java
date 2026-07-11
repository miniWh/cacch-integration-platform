package com.cacch.integration.dto.crm.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按订单 ID 查询明细请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class CrmOrderDetailQueryRequest {

    /**
     * 页码，默认 1
     */
    private Integer page;

    /**
     * 每页条数，默认 100
     */
    private Integer rows;

    /**
     * CRM 订单内部 ID（order.id）
     */
    @NotBlank(message = "crmOrderId 不能为空")
    private String crmOrderId;
}
