package com.cacch.integration.common.config.moka;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Moka 开放平台接入配置属性 —— 由 yml 的 {@code moka} 节点注入
 *
 * <p>配置 POJO 定义在 common 模块，由 web 模块的 {@code MokaConfiguration} 通过
 * {@code @EnableConfigurationProperties} 注册为 Bean。</p>
 *
 * <pre>
 * 绑定说明：
 * 构造器参数必须是「扁平标量」，Spring 构造器绑定会按参数名拼接前缀，
 * 即 {@code moka.base-url} / {@code moka.api-key} 直接绑定到
 * 本类的 baseUrl / apiKey；禁止嵌套 POJO 参与 Spring 绑定。
 * </pre>
 *
 * <pre>
 * 网关地址说明：
 * {@code baseUrl} 为 Moka 开放平台根地址（不含结尾斜杠），业务 URL 由
 * {@link com.cacch.integration.common.constant.moka.MokaConstants} 中的路径常量
 * 在运行时拼接。yml 未配置或为空白时回退到 {@link #DEFAULT_BASE_URL}；
 * 部署环境差异一律通过 yml 或环境变量 {@code MOKA_BASE_URL} 覆盖。
 * </pre>
 *
 * <p>敏感信息警告：{@code apiKey} 是 Moka 机构私密凭证，拥有访问所有 API 的权限，
 * 严禁打印到任何日志，禁止硬编码在代码中，必须由环境变量 {@code MOKA_API_KEY} 注入。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "moka")
public class MokaProperties {

    /**
     * Moka 开放平台网关根地址兜底值 —— yml 未配置 base-url 时使用
     */
    private static final String DEFAULT_BASE_URL = "https://api.mokahr.com";

    /**
     * Moka 开放平台网关根地址（自动去除结尾斜杠）
     */
    private final String baseUrl;

    /**
     * Moka 机构私密 API Key —— Basic Auth 鉴权的 username（password 为空）
     *
     * <p><strong>禁止记入日志！</strong>由环境变量 {@code MOKA_API_KEY} 注入。</p>
     */
    private final String apiKey;

    public MokaProperties(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl != null && !baseUrl.isBlank()
                ? trimTrailingSlash(baseUrl)
                : DEFAULT_BASE_URL;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    /**
     * API Key 是否已配置（非空且非空白）
     */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 去除 URL 结尾斜杠，避免与路径常量拼接时出现 {@code //}
     */
    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
