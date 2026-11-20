package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Moka OAuth2 获取 accessToken 请求体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaOAuth2TokenRequest {

    @JsonProperty("clientID")
    private String clientId;

    private String clientSecret;

    private String grantType;
}