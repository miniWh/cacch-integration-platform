package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 法大大创建用户响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddCreateAccountResponse {

    private Integer code;

    private String message;

    private FddCreateAccountData data;

    private Long timestamp;

    /**
     * 是否成功
     *
     * @return true 表示成功并返回 accountId
     */
    public boolean isSuccess() {
        return code != null
                && (code == FddConstants.SUCCESS_CODE || code == FddConstants.TOKEN_SUCCESS_CODE)
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
