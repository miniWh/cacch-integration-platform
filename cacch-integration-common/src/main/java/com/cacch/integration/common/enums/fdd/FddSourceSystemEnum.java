package com.cacch.integration.common.enums.fdd;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 法大大认证来源系统（审计字段）
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum FddSourceSystemEnum {

    /**
     * 勤策 CRM
     */
    CRM("CRM", "勤策 CRM"),

    /**
     * 致远 OA
     */
    OA("OA", "致远 OA");

    private final String code;
    private final String desc;

    /**
     * 按 code 解析枚举（大小写敏感，须精确匹配 CRM / OA）
     *
     * @param code 来源系统编码
     * @return 枚举；无法识别时返回 null
     */
    public static FddSourceSystemEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (FddSourceSystemEnum value : values()) {
            if (value.code.equals(code.trim())) {
                return value;
            }
        }
        return null;
    }
}
