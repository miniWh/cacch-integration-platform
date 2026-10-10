package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 法大大 OAuth2 Token 响应
 *
 * <p>通用字段（code / message / timestamp）继承自 {@link FddBaseResponse}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddTokenResponse extends FddBaseResponse {

    private FddTokenData data;

    /**
     * 是否成功（OAuth2 Token 接口成功码为 0，与 ESB/接口文档一致）
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        Integer c = getCode();
        return c != null && c == FddConstants.TOKEN_SUCCESS_CODE && data != null
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
