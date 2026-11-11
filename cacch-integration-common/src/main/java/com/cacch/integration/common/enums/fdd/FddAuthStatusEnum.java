package com.cacch.integration.common.enums.fdd;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 法大大认证状态
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum FddAuthStatusEnum {

    /**
     * 已发起，等待回调
     */
    PENDING("PENDING", "认证处理中"),

    /**
     * 认证通过
     */
    SUCCESS("SUCCESS", "认证通过"),

    /**
     * 认证失败
     */
    FAILED("FAILED", "认证失败");

    private final String code;
    private final String desc;
}
