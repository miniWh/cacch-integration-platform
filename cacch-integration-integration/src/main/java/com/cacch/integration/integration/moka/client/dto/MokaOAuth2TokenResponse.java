package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Moka OAuth2 获取 accessToken 响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MokaOAuth2TokenResponse extends MokaBaseResponse<MokaOAuth2TokenResponse.TokenData> {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenData {
        private String accessToken;
        private String tokenType;
        private Long expiresIn;
    }
}