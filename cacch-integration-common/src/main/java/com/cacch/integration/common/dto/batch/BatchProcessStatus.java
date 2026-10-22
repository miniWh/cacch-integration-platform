package com.cacch.integration.common.dto.batch;

import lombok.Getter;

/**
 * 批量处理明细状态
 *
 * <p>跨业务域通用的批量处理结果状态，供 {@link BatchProcessResult} 使用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public enum BatchProcessStatus {

    /**
     * 处理成功
     */
    SUCCESS("成功"),

    /**
     * 处理失败（异常或下游拒绝）
     */
    FAILURE("失败"),

    /**
     * 前置条件不满足主动跳过
     */
    SKIPPED("跳过");

    /**
     * 中文描述
     * -- GETTER --
     * 获取状态中文描述
     */
    private final String description;

    BatchProcessStatus(String description) {
        this.description = description;
    }
}
