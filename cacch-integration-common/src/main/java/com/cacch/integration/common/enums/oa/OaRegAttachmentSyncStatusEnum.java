package com.cacch.integration.common.enums.oa;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 国内登记报告附件同步状态
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@RequiredArgsConstructor
public enum OaRegAttachmentSyncStatusEnum {

    /**
     * 待同步
     */
    PENDING("PENDING", "待同步"),

    /**
     * 已成功
     */
    SUCCESS("SUCCESS", "已成功"),

    /**
     * 重试中
     */
    RETRY("RETRY", "重试中"),

    /**
     * 已失败（达最大重试）
     */
    FAILED("FAILED", "已失败"),

    /**
     * 已跳过（目录缺失/无文件/幂等等）
     */
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String desc;
}
