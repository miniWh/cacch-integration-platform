package com.cacch.integration.integration.fdd.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * 法大大创建企业请求（同时绑定系统管理员）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FddCreateCompanyRequest {

    /**
     * 企业名称
     */
    private String companyName;

    /**
     * 企业在第三方业务系统唯一标识（本场景使用 uscc）
     */
    private String tpOrgId;

    /**
     * 企业系统管理员姓名
     */
    private String adminName;

    /**
     * 企业系统管理员在第三方业务系统唯一标识（本场景使用管理员身份证号）
     */
    private String tpAccountId;

    /**
     * 企业系统管理员手机号
     */
    private String adminMobile;

    /**
     * 手机号区号，默认 +86
     */
    private String areaCode;
}
