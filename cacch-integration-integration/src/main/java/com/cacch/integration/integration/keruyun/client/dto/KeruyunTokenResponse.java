package com.cacch.integration.integration.keruyun.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客如云获取门店 token 响应
 *
 * <p>接口：{@code GET /open/v1/token/get}，成功码 {@code code=0}，token 位于 {@code result.token}。
 * 通用字段与成功判定继承 {@link KeruyunBaseResponse}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeruyunTokenResponse extends KeruyunBaseResponse<KeruyunTokenResponse.Result> {

    /**
     * 是否成功（code=0 且 result.token 非空）
     *
     * @return true 表示成功
     */
    @Override
    public boolean isSuccess() {
        Result data = getResult();
        return super.isSuccess() && data != null && data.getToken() != null && !data.getToken().isBlank();
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
