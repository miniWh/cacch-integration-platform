package com.cacch.integration.integration.keruyun.client.dto;

import com.cacch.integration.common.constant.keruyun.KeruyunConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 客如云获取门店 token 响应
 *
 * <p>接口：{@code GET /open/v1/token/get}，成功码 {@code code=0}，token 位于 {@code result.token}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeruyunTokenResponse {

    /**
     * 业务返回码，0 表示成功
     */
    private Integer code;

    /**
     * 返回说明
     */
    private String message;

    /**
     * 消息 UUID，便于与客如云排查问题
     */
    private String messageUuid;

    /**
     * 业务数据
     */
    private Result result;

    /**
     * 是否成功（code=0 且 result.token 非空）
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return code != null && code == KeruyunConstants.SUCCESS_CODE
                && result != null && result.getToken() != null && !result.getToken().isBlank();
    }

    /**
     * Token 业务数据
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {

        /**
         * 门店 token（参与业务接口签名）
         */
        private String token;
    }
}
