package com.cacch.integration.common.config.fdd;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 法大大配置属性（构造器绑定）
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "fdd")
public class FddProperties {

    private final String baseUrl;
    private final String authUrl;
    private final String appId;
    private final String appSecret;
    private final String enterpriseAuthUrl;
    private final String personAuthUrl;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final String callbackUrl;
    private final int callbackTimeoutMinutes;
    private final int tokenCacheMinutes;
    private final int enterpriseVerifiedWay;
    private final int personVerifiedWay;
    private final int maxRetry;
    private final List<String> internalCompanies;

    /**
     * 构造器绑定
     *
     * @param baseUrl                法大大服务根地址
     * @param authUrl                OAuth2 Token 地址
     * @param appId                  应用 ID
     * @param appSecret              应用密钥（环境变量注入）
     * @param enterpriseAuthUrl      企业认证 URL 接口
     * @param personAuthUrl          个人认证 URL 接口
     * @param connectTimeoutSeconds  连接超时秒数
     * @param readTimeoutSeconds     读取超时秒数
     * @param callbackUrl            回调通知地址
     * @param callbackTimeoutMinutes 回调超时分钟数
     * @param tokenCacheMinutes      Token 缓存分钟数
     * @param enterpriseVerifiedWay  企业认证方案
     * @param personVerifiedWay      个人认证方案
     * @param maxRetry               最大重试次数
     * @param internalCompanies      内部企业全称白名单
     */
    public FddProperties(String baseUrl,
                         String authUrl,
                         String appId,
                         String appSecret,
                         String enterpriseAuthUrl,
                         String personAuthUrl,
                         Integer connectTimeoutSeconds,
                         Integer readTimeoutSeconds,
                         String callbackUrl,
                         Integer callbackTimeoutMinutes,
                         Integer tokenCacheMinutes,
                         Integer enterpriseVerifiedWay,
                         Integer personVerifiedWay,
                         Integer maxRetry,
                         List<String> internalCompanies) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.authUrl = authUrl;
        this.appId = appId;
        this.appSecret = appSecret;
        this.enterpriseAuthUrl = enterpriseAuthUrl;
        this.personAuthUrl = personAuthUrl;
        this.connectTimeoutSeconds = connectTimeoutSeconds != null ? connectTimeoutSeconds : 10;
        this.readTimeoutSeconds = readTimeoutSeconds != null ? readTimeoutSeconds : 30;
        this.callbackUrl = callbackUrl;
        this.callbackTimeoutMinutes = callbackTimeoutMinutes != null ? callbackTimeoutMinutes : 3;
        this.tokenCacheMinutes = tokenCacheMinutes != null ? tokenCacheMinutes : 25;
        this.enterpriseVerifiedWay = enterpriseVerifiedWay != null ? enterpriseVerifiedWay : 0;
        this.personVerifiedWay = personVerifiedWay != null ? personVerifiedWay : 0;
        this.maxRetry = maxRetry != null ? maxRetry : 3;
        this.internalCompanies = internalCompanies != null ? List.copyOf(internalCompanies) : List.of();
    }

    /**
     * 判断内部企业全称是否在白名单内（精确匹配）
     *
     * @param companyName 内部企业全称
     * @return true 表示允许
     */
    public boolean isAllowedInternalCompany(String companyName) {
        if (!StringUtils.hasText(companyName)) {
            return false;
        }
        String trimmed = companyName.trim();
        return internalCompanies.stream().anyMatch(trimmed::equals);
    }

    private static String trimTrailingSlash(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
