package com.cacch.integration.integration.fdd.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 法大大企业实名认证 URL 请求体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FddEnterpriseAuthRequest {

    /**
     * 法大大本地企业唯一标识（创建企业后回填；有则优先传）
     */
    private String companyId;

    /**
     * 企业系统管理员法大大本地用户标识（创建企业绑定后回填）
     */
    private String accountId;

    /**
     * 第三方组织唯一标识，本场景使用 uscc
     */
    private String tpOrgId;

    /**
     * 认证渠道，固定 0
     */
    private Integer verifiedChannel;

    /**
     * 企业认证方案
     */
    private Integer verifiedWay;

    /**
     * 1 首次认证，2 重新认证
     */
    private Integer isRepeatVerified;

    /**
     * 企业信息
     */
    private CompanyInfoDTO companyInfoDTO;

    /**
     * 企业管理员身份：0 全部
     */
    private Integer applicationType;

    /**
     * 异步回调地址
     */
    private String notifyUrl;

    /**
     * 是否发送短信：0 否
     */
    private Integer isSendSms;

    /**
     * 页面是否可修改：2 不允许
     */
    private Integer pageModify;

    /**
     * 企业信息 DTO
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompanyInfoDTO {

        @JsonProperty("companyName")
        private String companyName;

        @JsonProperty("creditCode")
        private String creditCode;
    }
}
