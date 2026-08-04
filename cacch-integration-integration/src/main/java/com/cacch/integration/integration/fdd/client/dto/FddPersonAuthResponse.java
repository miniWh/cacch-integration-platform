package com.cacch.integration.integration.fdd.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 法大大个人实名认证 URL 响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddPersonAuthResponse {

    private Integer code;

    private String message;

    private FddAuthUrlData data;

    private Long timestamp;

    /**
     * 是否成功
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return code != null && code == 100000 && data != null
                && data.getUrl() != null && !data.getUrl().isBlank();
    }

    /**
     * 认证 URL 数据
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FddAuthUrlData {

        private String url;

        @JsonProperty("transactionNo")
        private String transactionNo;
    }
}
