package com.cacch.integration.common.enums.crm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CRM 明细 OA 同步状态
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum CrmOaSyncStatusEnum {

    PENDING("PENDING", "待同步"),
    SUCCESS("SUCCESS", "已成功"),
    RETRY("RETRY", "重试中"),
    FAILED("FAILED", "已失败");

    private final String code;
    private final String desc;
}
