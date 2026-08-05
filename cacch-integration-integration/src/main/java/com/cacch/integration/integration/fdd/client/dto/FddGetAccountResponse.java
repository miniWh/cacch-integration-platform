package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 法大大查询用户详情响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddGetAccountResponse {

    private Integer code;

    private String message;

    private FddAccountData data;

    private Long timestamp;

    /**
     * 是否业务成功且有用户数据
     *
     * @return true 表示查到用户
     */
    public boolean hasAccount() {
        return code != null
                && (code == FddConstants.SUCCESS_CODE || code == FddConstants.TOKEN_SUCCESS_CODE)
                && data != null
                && data.getAccountId() != null
                && !data.getAccountId().isBlank();
    }

    /**
     * 用户是否已实名
     *
     * @return true 表示已认证
     */
    public boolean isCertified() {
        return hasAccount()
                && FddConstants.PERSON_VERIFY_CERTIFIED.equals(data.getVerifyStatus());
    }

    /**
     * 用户详情
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FddAccountData {

        @JsonProperty("accountId")
        private String accountId;

        @JsonProperty("tpAccountId")
        private String tpAccountId;

        @JsonProperty("userName")
        private String userName;

        private String mobile;

        /**
         * 认证状态：0未认证,1已认证,2认证失败,3认证中,4待授权,5认证失效,6授权失败
         */
        @JsonProperty("verifyStatus")
        private String verifyStatus;
    }
}
