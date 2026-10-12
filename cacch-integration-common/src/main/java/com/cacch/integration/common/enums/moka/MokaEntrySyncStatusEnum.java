package com.cacch.integration.common.enums.moka;

import lombok.Getter;

/**
 * Moka 待入职 iHR 同步状态枚举
 *
 * <p>对应表 {@code t_integration_moka_entry_sync.ihr_sync_status} 字段值。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public enum MokaEntrySyncStatusEnum {

    /**
     * 待同步 —— 第一步快照入库时初始状态
     */
    PENDING("PENDING", "待同步"),

    /**
     * 同步成功 —— iHR entryForms/create 返回 code=0
     */
    SUCCESS("SUCCESS", "同步成功"),

    /**
     * 同步失败 —— iHR 返回业务码非 0 或 HTTP 异常
     */
    FAILED("FAILED", "同步失败"),

    /**
     * 跳过 —— 部门编码缺失、入职日期缺失等前置条件不满足
     */
    SKIPPED("SKIPPED", "跳过");

    private final String code;
    private final String desc;

    MokaEntrySyncStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 按 code 值查枚举
     *
     * @param code 状态码
     * @return 枚举值，code 为 null 或无匹配时返回 null
     */
    public static MokaEntrySyncStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MokaEntrySyncStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
