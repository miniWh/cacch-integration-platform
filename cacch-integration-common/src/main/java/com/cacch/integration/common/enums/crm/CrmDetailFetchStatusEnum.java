package com.cacch.integration.common.enums.crm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CRM 订单明细拉取状态
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum CrmDetailFetchStatusEnum {

    PENDING("PENDING", "待拉取"),
    SUCCESS("SUCCESS", "已成功"),
    FAILED("FAILED", "失败待补拉");

    private final String code;
    private final String desc;
}
