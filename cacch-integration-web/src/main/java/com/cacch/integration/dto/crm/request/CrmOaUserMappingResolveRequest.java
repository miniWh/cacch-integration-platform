package com.cacch.integration.dto.crm.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * CRM↔OA 人员映射解析请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class CrmOaUserMappingResolveRequest {

    /**
     * CRM 员工 ID（订单 owner.id），必填
     */
    @NotBlank(message = "crmEmployeeId 不能为空")
    private String crmEmployeeId;

    /**
     * 是否强制刷新（忽略库表有效缓存）
     */
    private Boolean forceRefresh;
}
