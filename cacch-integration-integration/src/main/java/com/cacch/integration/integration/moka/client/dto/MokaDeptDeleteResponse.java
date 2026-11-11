package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka 删除及合并组织架构 —— 响应体
 *
 * <p>字段匹配 Moka API {@code DELETE /api-platform/v1/departments} 的响应结构
 * {@code {success, errorMessage}}。批量删除语义为整体成败（success 布尔），
 * 不提供逐条结果。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptDeleteResponse {

    /**
     * 本次删除是否整体成功（null 安全判定见 {@link #isSuccess()}）
     */
    private Boolean success;

    /**
     * 失败原因（success=true 时为 null）
     */
    private String errorMessage;

    /**
     * 成功判定 —— success 为 Boolean，需 null 安全比较
     *
     * @return success 非空且为 true 时返回 true
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}
