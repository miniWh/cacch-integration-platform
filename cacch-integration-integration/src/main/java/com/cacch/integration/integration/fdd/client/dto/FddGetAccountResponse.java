package com.cacch.integration.integration.fdd.client.dto;

import com.cacch.integration.common.constant.fdd.FddConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 法大大查询用户详情响应
 *
 * <p>私有化部署实际返回 {@code data} 为数组；取首条有效用户。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddGetAccountResponse {

    private Integer code;

    private String message;

    /**
     * 用户列表（接口文档写 object，实际多为 array；兼容 object）
     */
    @JsonDeserialize(using = FddObjectOrArrayDeserializer.class)
    private List<FddAccountData> data;

    private Long timestamp;

    /**
     * 取首条用户数据
     *
     * @return 首条用户；无数据时返回 null
     */
    public FddAccountData firstAccount() {
        if (CollectionUtils.isEmpty(data)) {
            return null;
        }
        return data.getFirst();
    }

    /**
     * 是否业务成功且有用户数据
     *
     * @return true 表示查到用户
     */
    public boolean hasAccount() {
        if (code == null
                || (code != FddConstants.SUCCESS_CODE && code != FddConstants.TOKEN_SUCCESS_CODE)) {
            return false;
        }
        FddAccountData account = firstAccount();
        return account != null && StringUtils.hasText(account.getAccountId());
    }

    /**
     * 用户是否已实名
     *
     * @return true 表示已认证
     */
    public boolean isCertified() {
        FddAccountData account = firstAccount();
        return hasAccount()
                && account != null
                && FddConstants.PERSON_VERIFY_CERTIFIED.equals(account.getVerifyStatus());
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
