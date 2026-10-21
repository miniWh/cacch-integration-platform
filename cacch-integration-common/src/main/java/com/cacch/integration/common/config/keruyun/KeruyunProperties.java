package com.cacch.integration.common.config.keruyun;

import com.cacch.integration.common.constant.keruyun.KeruyunConstants;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 客如云开放平台接入配置属性（构造器绑定）
 *
 * <p>由 yml 的 {@code keruyun} 节点注入，secret 经环境变量 {@code KERUYUN_APP_SECRET} 注入，
 * 严禁硬编码与打印到日志。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "keruyun")
public class KeruyunProperties {

    /**
     * 客如云开放平台网关根地址（自动去除结尾斜杠）
     */
    private final String baseUrl;

    /**
     * 开发者 appKey
     */
    private final String appKey;

    /**
     * 门店 ID（门店授权维度，与品牌授权参数 brandId 互斥）
     */
    private final String shopIdenty;

    /**
     * 开发者密钥 secretKey，仅用于签名，禁止记录日志
     */
    private final String appSecret;

    /**
     * 接口版本，固定 2.0
     */
    private final String version;

    /**
     * Token Redis 缓存 TTL（秒）
     */
    private final long tokenCacheSeconds;

    /**
     * 构造器绑定
     *
     * @param baseUrl           客如云网关根地址
     * @param appKey            开发者 appKey
     * @param shopIdenty        门店 ID
     * @param appSecret         开发者密钥（环境变量注入，禁止日志输出）
     * @param version           接口版本，为空时使用 2.0
     * @param tokenCacheSeconds Token 缓存 TTL 秒数，为空或非正数时使用 86400
     */
    public KeruyunProperties(String baseUrl,
                             String appKey,
                             String shopIdenty,
                             String appSecret,
                             String version,
                             Long tokenCacheSeconds) {
        this.baseUrl = trimTrailingSlash(
                StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://openapi.keruyun.com");
        this.appKey = appKey != null ? appKey.trim() : "";
        this.shopIdenty = shopIdenty != null ? shopIdenty.trim() : "";
        this.appSecret = appSecret != null ? appSecret.trim() : "";
        this.version = StringUtils.hasText(version) ? version.trim() : KeruyunConstants.DEFAULT_VERSION;
        this.tokenCacheSeconds = tokenCacheSeconds != null && tokenCacheSeconds > 0
                ? tokenCacheSeconds : KeruyunConstants.TOKEN_DEFAULT_TTL_SECONDS;
    }

    /**
     * 判断鉴权配置是否完整（appKey / shopIdenty / appSecret 均已配置）
     *
     * @return true 表示可发起客如云鉴权请求
     */
    public boolean isAuthConfigured() {
        return !StringUtils.hasText(appKey) || !StringUtils.hasText(shopIdenty) || !StringUtils.hasText(appSecret);
    }

    /**
     * 去除 URL 结尾斜杠，避免与接口路径拼接时出现双斜杠
     */
    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
