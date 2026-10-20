package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 标记候选人已入职响应体
 *
 * <p>对接 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/hired} 的返回结构：
 * <pre>
 * {
 *   "success": true,          // boolean，判断调用结果
 *   "errorMessage": "..."     // string，报错信息
 * }
 * </pre></p>
 *
 * <p>说明：本接口返回结构与 {@link MokaBaseResponse}（int code + msg）及
 * {@link MokaApplicationResignResponse}（String code）均不兼容，
 * 故按 Moka 域内「特殊结构保持独立」约定单独定义，不纳入任何基类。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaApplicationHiredResponse {

    /**
     * 是否成功（boolean），用于判断调用结果
     */
    private Boolean success;

    /**
     * 报错信息，调用失败时返回
     */
    private String errorMessage;

    /**
     * 判断调用是否成功
     *
     * <p>{@code success} 为 null（字段缺失）时视为失败，避免空值误判为成功。</p>
     *
     * @return success 为 {@code Boolean.TRUE} 时返回 true，否则返回 false
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}
