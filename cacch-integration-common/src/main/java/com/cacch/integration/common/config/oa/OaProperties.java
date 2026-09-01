package com.cacch.integration.common.config.oa;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 致远 OA REST 配置属性
 *
 * <p>采用构造器绑定：单一构造器即被 Spring Boot 自动识别为目标构造器，所有参数均通过
 * {@link DefaultValue} 在 yml 缺失对应 key 时回退默认值；{@link #restPassword}
 * 不设默认字符串，必须由外部配置注入，使用方会在缺失时主动校验失败。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "oa")
public class OaProperties {

    /**
     * OA 服务根地址，如 {@code http://oa.example.com}（不含末尾斜杠）
     */
    private final String baseUrl;

    /**
     * REST 账号用户名（path 中的 userName）
     */
    private final String restUserName;

    /**
     * REST 账号密码（path 中的 password，禁止写入日志；无默认值，须外部配置注入）
     */
    private final String restPassword;

    /**
     * 默认绑定登录名（Token 的 loginName）；为空则不带 loginName 参数
     */
    private final String defaultLoginName;

    /**
     * Token Redis 缓存秒数；致远默认约 15 分钟，建议略小于该值
     */
    private final long tokenTtlSeconds;

    /**
     * 默认表单模板编号（发起流程时可被请求覆盖）
     */
    private final String templateCode;

    /**
     * OA 附件上传读超时（秒）；大文件流式上传需较长超时，默认 30 分钟
     */
    private final int attachmentUploadReadTimeoutSeconds;

    public OaProperties(
            @DefaultValue("http://10.80.64.18:900") String baseUrl,
            @DefaultValue("zhouhufu") String restUserName,
            String restPassword,
            @DefaultValue("zhouhongfu") String defaultLoginName,
            @DefaultValue("840") Long tokenTtlSeconds,
            @DefaultValue("CRM_ZYXS_001") String templateCode,
            @DefaultValue("1800") Integer attachmentUploadReadTimeoutSeconds) {
        this.baseUrl = baseUrl;
        this.restUserName = restUserName;
        // restPassword 无 @DefaultValue：缺失时传 null，使用方需自行校验
        this.restPassword = restPassword;
        this.defaultLoginName = defaultLoginName;
        this.tokenTtlSeconds = tokenTtlSeconds;
        this.templateCode = templateCode;
        this.attachmentUploadReadTimeoutSeconds = attachmentUploadReadTimeoutSeconds;
    }

    /**
     * 规范化后的 Base URL（去掉末尾 /）
     *
     * @return Base URL
     */
    public String resolvedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://placeholder-oa-host";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
