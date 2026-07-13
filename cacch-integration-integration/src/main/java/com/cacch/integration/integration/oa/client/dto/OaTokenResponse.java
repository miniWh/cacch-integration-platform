package com.cacch.integration.integration.oa.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 致远 OA Token 响应
 *
 * <p>Accept=application/json 时通常返回 {@code {"id":"token值"}}；部分环境可能直接返回纯文本。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OaTokenResponse {

    /**
     * Token 值（字段名 id）
     */
    @JsonProperty("id")
    private String id;

    /**
     * 部分版本可能用 token 字段
     */
    private String token;

    /**
     * 解析可用的 Token 字符串
     *
     * @return Token；均空时返回 null
     */
    public String resolveToken() {
        if (id != null && !id.isBlank()) {
            return id.trim();
        }
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        return null;
    }
}
