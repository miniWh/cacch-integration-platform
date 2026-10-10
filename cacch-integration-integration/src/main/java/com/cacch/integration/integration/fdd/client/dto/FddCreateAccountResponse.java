package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 法大大创建用户响应
 *
 * <p>通用字段（code / message / timestamp）继承自 {@link FddBaseResponse}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddCreateAccountResponse extends FddBaseResponse {

    private FddCreateAccountData data;

    /**
     * 是否成功
     *
     * @return true 表示成功并返回 accountId
     */
    public boolean isSuccess() {
        Integer c = getCode();
        return c != null
                && (c == FddConstants.SUCCESS_CODE || c == FddConstants.TOKEN_SUCCESS_CODE)
                && data != null
                && data.getAccountId() != null
                && !data.getAccountId().isBlank();
    }

    /**
     * 创建用户数据
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FddCreateAccountData {

        @JsonProperty("accountId")
        private String accountId;
    }
}
