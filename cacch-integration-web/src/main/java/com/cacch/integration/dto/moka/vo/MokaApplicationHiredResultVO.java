package com.cacch.integration.dto.moka.vo;

import lombok.Data;

/**
 * Moka 标记候选人已入职 —— 结果 VO
 *
 * <p>返回 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/hired}
 * 的调用结果。Moka 该接口直接返回 {@code success} 布尔字段，本 VO 原样透传。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaApplicationHiredResultVO {

    /**
     * Moka 返回的调用结果：success=true 表示操作成功
     */
    private Boolean success;

    /**
     * Moka 原始报错信息，失败时返回
     */
    private String errorMessage;
}
