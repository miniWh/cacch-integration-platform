package com.cacch.integration.common.enums.fdd;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 法大大认证类型
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum FddAuthTypeEnum {

    /**
     * 企业实名认证
     */
    ENTERPRISE("ENTERPRISE", "企业实名认证"),

    /**
     * 个人实名认证
     */
    PERSON("PERSON", "个人实名认证");

    private final String code;
    private final String desc;

    /**
     * 按 code 解析枚举
     *
     * @param code 认证类型编码
     * @return 枚举；无法识别时返回 null
     */
    public static FddAuthTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (FddAuthTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        return null;
    }
}
