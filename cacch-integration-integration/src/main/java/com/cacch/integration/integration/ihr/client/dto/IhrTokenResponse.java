package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * IHR OAuth2 Token 响应（同时覆盖获取与刷新两种场景）
 *
 * <p>成功响应示例：
 * <pre>
 * {
 *   "access_token": "c237c59c-a7a2-4a24-922c-af09760a9a68",
 *   "token_type": "bearer",
 *   "refresh_token": "394162b1-0127-418e-1379-15c816651f4f",
 *   "expires_in": 4431,
 *   "scope": "client"
 * }
 * </pre>
 *
 * <p>失败响应示例：
 * <pre>
 * {
 *   "error": "invalid_token",
 *   "error_description": "Access token expired..."
 * }
 * </pre>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrTokenResponse {

    /**
     * 访问令牌；用于业务接口 {@code Authorization: Bearer {access_token}}
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 令牌类型，固定 {@code bearer}
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * 刷新令牌；用于 grant_type=refresh_token 续期 access_token
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * access_token 有效时长（秒），IHR 官方约定 7200s
     */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * 授权范围
     */
    private String scope;

    /**
     * 错误码（失败响应），如 {@code invalid_token}
     */
    private String error;

    /**
     * 错误描述（失败响应）
     */
    @JsonProperty("error_description")
    private String errorDescription;

    /**
     * 是否成功响应
     */
    public boolean isSuccess() {
        return accessToken != null && !accessToken.isBlank() && error == null;
    }
}
