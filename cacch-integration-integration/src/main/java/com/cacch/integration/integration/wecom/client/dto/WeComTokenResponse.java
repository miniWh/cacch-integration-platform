package com.cacch.integration.integration.wecom.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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
 * @author hongfu_zhou@cacch.com
 */
@Data
public class WeComTokenResponse {

    /**
     * 错误码，0 表示成功
     */
    @JsonProperty("errcode")
    private int errCode;

    /**
     * 错误信息
     */
    @JsonProperty("errmsg")
    private String errMsg;

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

    /**
     * 是否为成功响应
     */
    public boolean isSuccess() {
        return errCode == 0;
    }
}
