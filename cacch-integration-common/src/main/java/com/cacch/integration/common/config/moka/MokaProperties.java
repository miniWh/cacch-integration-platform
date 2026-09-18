package com.cacch.integration.common.config.moka;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Moka 开放平台接入配置属性 —— 由 yml 的 {@code moka} 节点注入，环境变量兜底
 *
 * <p>配置 POJO 定义在 common 模块，由 web 模块的 {@code MokaConfiguration} 通过
 * {@code @EnableConfigurationProperties} 注册为 Bean。</p>
 *
 * <pre>
 * 绑定说明：
 * 构造器参数必须是「扁平标量」，Spring 构造器绑定会按参数名拼接前缀，
 * 即 {@code moka.base-url} / {@code moka.api-key} 直接绑定到本类字段；
 * 禁止嵌套 POJO 参与 Spring 绑定。
 * </pre>
 *
 * <pre>
 * 环境变量兜底逻辑（关键）：
 * 由于启动命令通过 {@code --spring.config.additional-location=file:...application-test.yml}
 * 加载外部 yml，若外部 yml 中存在 moka 节点但 api-key 字段为 null/空串，会覆盖 jar 内部
 * 的 {@code ${MOKA_API_KEY:}} 占位符，导致 Spring 无法解析环境变量。
 *
 * 本类构造器在 yml 绑定值为 null 或空白时，主动调用 {@link System#getenv(String)}
 * 读取环境变量作为兜底，与启动脚本中的 {@code export MOKA_API_KEY=xxx} 直接对接，
 * 无需修改外部 yml 文件。
 * </pre>
 *
 * <p>鉴权方式：HTTP Basic Auth，API Key 作为 username（password 为空），
 * 由脚本 {@code export MOKA_API_KEY} 环境变量注入。</p>
 *
 * <p>敏感信息警告：{@code apiKey} 是 Moka 机构私密凭证，拥有访问所有 API 的权限，
 * 严禁打印到任何日志，禁止硬编码在代码中，必须由环境变量注入。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "moka")
public class MokaProperties {

    /**
     * Moka 开放平台网关根地址兜底值 —— yml 未配置且环境变量也未配置时使用
     */
    private static final String DEFAULT_BASE_URL = "https://api.mokahr.com";

    /**
     * 环境变量名 —— Moka 机构私密 API Key
     */
    public static final String ENV_MOKA_API_KEY = "MOKA_API_KEY";

    /**
     * 环境变量名 —— Moka 开放平台网关根地址（可选）
     */
    public static final String ENV_MOKA_BASE_URL = "MOKA_BASE_URL";

    /**
     * Moka 开放平台网关根地址（自动去除结尾斜杠）
     *
     * <p>取值优先级：yml {@code moka.base-url} → 环境变量 {@code MOKA_BASE_URL} → 兜底值
     * {@code https://api.mokahr.com}</p>
     */
    private final String baseUrl;

    /**
     * Moka 机构私密 API Key —— Basic Auth 鉴权的 username（password 为空）
     *
     * <p>取值优先级：yml {@code moka.api-key} → 环境变量 {@code MOKA_API_KEY} → 空串
     *
     * <p><strong>禁止记入日志！</strong></p>
     */
    private final String apiKey;

    public MokaProperties(String baseUrl, String apiKey) {
        // baseUrl：yml → 环境变量 → 兜底
        this.baseUrl = resolveBaseUrl(baseUrl);
        // apiKey：yml → 环境变量（yml 为 null/空串时主动读 System.getenv）
        this.apiKey = resolveApiKey(apiKey);
    }

    /**
     * API Key 是否已配置（非空且非空白）
     */
    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 解析 baseUrl：yml 值优先，null/空白时回退环境变量，再兜底默认地址
     */
    private static String resolveBaseUrl(String ymlValue) {
        String candidate = ymlValue;
        if (candidate == null || candidate.isBlank()) {
            candidate = System.getenv(ENV_MOKA_BASE_URL);
        }
        if (candidate == null || candidate.isBlank()) {
            candidate = DEFAULT_BASE_URL;
        }
        return trimTrailingSlash(candidate.trim());
    }

    /**
     * 解析 apiKey：yml 值优先，null/空白时回退环境变量
     *
     * <p>关键兜底：当外部 yml 覆盖 jar 内部占位符导致 Spring 绑定 null 时，
     * 直接读 {@link System#getenv(String)}，确保脚本 export 的值能生效。</p>
     */
    private static String resolveApiKey(String ymlValue) {
        if (ymlValue != null && !ymlValue.isBlank()) {
            return ymlValue.trim();
        }
        String envValue = System.getenv(ENV_MOKA_API_KEY);
        return envValue != null ? envValue.trim() : "";
    }

    /**
     * 去除 URL 结尾斜杠，避免与路径常量拼接时出现 {@code //}
     */
    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
