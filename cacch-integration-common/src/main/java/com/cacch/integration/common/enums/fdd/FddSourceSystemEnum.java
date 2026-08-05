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
    OA("OA", "致远 OA"),

    /**
     * 从法大大侧查询同步落库（非业务系统主动发起）
     */
    SYNC("SYNC", "法大大侧同步");

    private final String code;
    private final String desc;

    /**
     * 按 code 解析枚举（大小写敏感）
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

    /**
     * 发起认证时仅允许 CRM / OA
     *
     * @param code 来源系统编码
     * @return 枚举；非 CRM/OA 时返回 null
     */
    public static FddSourceSystemEnum fromInitiateCode(String code) {
        FddSourceSystemEnum value = fromCode(code);
        if (value == CRM || value == OA) {
            return value;
        }
        return null;
    }
}
