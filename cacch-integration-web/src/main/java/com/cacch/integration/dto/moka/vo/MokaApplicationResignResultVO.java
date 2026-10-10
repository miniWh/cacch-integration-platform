package com.cacch.integration.dto.moka.vo;

import lombok.Data;

/**
 * Moka 标记候选人已离职 —— 结果 VO
 *
 * <p>返回 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/resign}
 * 的调用结果。注意 Moka 官方已废弃响应中的 {@code success} 字段，
 * 本 VO 的 {@code success} 由本服务根据 {@code code} 计算得出，非 Moka 原始字段。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaApplicationResignResultVO {

    /**
     * 本服务判定的调用结果：code 命中成功码（0/200/success）时为 true
     */
    private Boolean success;

    /**
     * Moka 原始返回码（string），透传用于排查
     */
    private String code;

    /**
     * Moka 原始报错信息，失败时返回
     */
    private String errorMessage;
}
