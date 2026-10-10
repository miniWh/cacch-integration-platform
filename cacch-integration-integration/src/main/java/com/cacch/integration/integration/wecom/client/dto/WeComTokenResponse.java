package com.cacch.integration.integration.wecom.client.dto;

import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业微信 /gettoken API 返回 DTO
 *
 * <pre>
 * 成功响应示例：
 * {
 *   "errcode": 0,
 *   "errmsg": "ok",
 *   "access_token": "accesstoken000001",
 *   "expires_in": 7200
 * }
 * </pre>
 *
 * <p>通用字段（errCode / errMsg / isSuccess）继承自 {@link WeComBaseResponse}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeComTokenResponse extends WeComBaseResponse {

    /**
     * access_token 值
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 有效期，单位：秒（企微固定返回 7200）
     */
    @JsonProperty("expires_in")
    private int expiresIn;
}
