package com.cacch.integration.integration.fdd.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 法大大认证结果回调请求体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FddCallbackRequest {

    /**
     * 通知类型：ENTERPRISE_IDENTIFY / PERSONAL_IDENTIFY
     */
    private String notifyType;

    /**
     * 企业认证流水号
     */
    private String transactionNo;

    /**
     * 法大大本地企业 ID
     */
    private String companyId;

    /**
     * 第三方组织 ID（uscc）
     */
    private String tpOrgId;

    /**
     * 法大大本地用户 ID
     */
    private String accountId;

    /**
     * 第三方账号 ID（身份证号）
     */
    private String tpAccountId;

    /**
     * 认证状态码。
     * 企业：3 已认证，4 认证失败；个人：1 已认证，2 认证失败
     */
    private Integer status;
}
