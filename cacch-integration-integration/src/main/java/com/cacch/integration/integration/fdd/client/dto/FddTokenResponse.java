package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 法大大 OAuth2 Token 响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddTokenResponse {

    private Integer code;

    private String message;

    private FddTokenData data;

    private Long timestamp;

    /**
     * 是否成功（OAuth2 Token 接口成功码为 0，与 ESB/接口文档一致）
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return code != null && code == FddConstants.TOKEN_SUCCESS_CODE && data != null
                && data.getAccessToken() != null && !data.getAccessToken().isBlank();
    }

    /**
     * Token 数据
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FddTokenData {

        @JsonProperty("accessToken")
        private String accessToken;

        @JsonProperty("expiresIn")
        private Long expiresIn;
    }
}
