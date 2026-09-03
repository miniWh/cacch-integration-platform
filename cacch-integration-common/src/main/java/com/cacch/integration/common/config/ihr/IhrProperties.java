package com.cacch.integration.common.config.ihr;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IHR 开放平台接入配置属性 — 由 yml 的 {@code ihr} 节点注入
 *
 * <p>配置 POJO 定义在 common 模块，由 web 模块的 {@code IhrConfiguration} 通过
 * {@code @EnableConfigurationProperties} 注册为 Bean。</p>
 *
 * <pre>
 * 绑定说明（重要）：
 * 构造器参数必须是「扁平标量」而非嵌套对象。Spring 构造器绑定会按参数名拼接前缀，
 * 即 {@code ihr.base-url} / {@code ihr.app-key} / {@code ihr.app-secret} 直接绑定到
 * 本类的 baseUrl / appKey / appSecret；
 * 若写成 {@code IhrProperties(IhrAppConfig credential)}，Spring 会去找 {@code ihr.credential.*}，
 * 导致 appKey / appSecret 静默为 null（凭证失效且无报错，排查成本极高）。
 * {@link IhrAppConfig} 仅作为值对象在本类内部组装，不参与 Spring 绑定。
 * </pre>
 *
 * <pre>
 * 网关地址说明：
 * {@code baseUrl} 为 IHR 开放平台根地址（不含结尾斜杠），业务 URL 由
 * {@code IhrConstants} 中的路径常量在运行时拼接，详见
 * {@code IhrTokenClient} / {@code IhrOrgClient}。
 * yml 未配置或为空白时回退到 {@link #DEFAULT_BASE_URL}，
 * 部署环境差异（测试 / 生产 / 内网域名切换）一律通过 yml 或环境变量
 * {@code IHR_BASE_URL} 覆盖，禁止改代码重新打包。
 * </pre>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "ihr")
public class IhrProperties {

    /**
     * IHR 开放平台根地址兜底值 —— yml 未配置 base-url 时使用
     */
    private static final String DEFAULT_BASE_URL = "http://10.80.87.11";

    /**
     * IHR 开放平台网关根地址（自动去除结尾斜杠）
     * -- GETTER --
     * 获取 IHR 开放平台网关根地址
     */
    private final String baseUrl;

    /**
     * IHR 接入凭证
     * -- GETTER --
     * 获取 IHR 接入凭证对象
     * IHR 凭证；yml 未配置时返回空对象（appKey / appSecret 均为 null）
     */
    private final IhrAppConfig credential;

    public IhrProperties(String baseUrl, String appKey, String appSecret) {
        this.baseUrl = baseUrl != null && !baseUrl.isBlank()
                ? trimTrailingSlash(baseUrl)
                : DEFAULT_BASE_URL;
        this.credential = new IhrAppConfig(appKey, appSecret);
    }

    /**
     * 去除 URL 结尾斜杠，避免与路径常量拼接时出现 {@code //}
     */
    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
