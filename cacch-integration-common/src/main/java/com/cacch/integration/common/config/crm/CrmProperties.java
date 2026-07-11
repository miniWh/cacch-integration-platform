package com.cacch.integration.common.config.crm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 勤策 CRM OpenAPI 配置属性
 *
 * @author hongfu_zhou@cacch.com
 */
@ConfigurationProperties(prefix = "crm")
public class CrmProperties {

    /**
     * OpenAPI Base URL
     */
    private final String baseUrl;

    /**
     * 企业接入唯一授权标识 openid
     */
    private final String openId;

    /**
     * 数据签名密钥 appkey（禁止写入日志）
     */
    private final String appKey;

    /**
     * 员工接口 Base URL；为空时复用 {@link #baseUrl}
     */
    private final String employeeBaseUrl;

    public CrmProperties(String baseUrl, String openId, String appKey, String employeeBaseUrl) {
        this.baseUrl = baseUrl != null && !baseUrl.isBlank()
                ? trimTrailingSlash(baseUrl)
                : "https://crm.cacch.com:50001";
        this.openId = openId;
        this.appKey = appKey;
        this.employeeBaseUrl = employeeBaseUrl != null && !employeeBaseUrl.isBlank()
                ? trimTrailingSlash(employeeBaseUrl)
                : this.baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getOpenId() {
        return openId;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getEmployeeBaseUrl() {
        return employeeBaseUrl;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
