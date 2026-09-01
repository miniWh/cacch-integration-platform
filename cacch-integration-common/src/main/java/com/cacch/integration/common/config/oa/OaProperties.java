package com.cacch.integration.common.config.oa;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 致远 OA REST 配置属性
 *
 * <p>采用构造器绑定：{@code private final} 字段 + 显式构造器，未配置项回退默认值。
 * 敏感字段（{@link #restPassword}）不设默认值，必须由外部配置注入，缺失时由上层在使用处校验。</p>
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
     * REST 账号密码（path 中的 password，禁止写入日志；无默认值，须外部配置）
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

    public OaProperties(String baseUrl,
                        String restUserName,
                        String restPassword,
                        String defaultLoginName,
                        Long tokenTtlSeconds,
                        String templateCode,
                        Integer attachmentUploadReadTimeoutSeconds) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://10.80.64.18:900";
        this.restUserName = restUserName != null ? restUserName : "zhouhufu";
        this.restPassword = restPassword;
        this.defaultLoginName = defaultLoginName;
        this.tokenTtlSeconds = tokenTtlSeconds != null ? tokenTtlSeconds : 840L;
        this.templateCode = templateCode != null ? templateCode : "CRM_ZYXS_001";
        this.attachmentUploadReadTimeoutSeconds = attachmentUploadReadTimeoutSeconds != null
                ? attachmentUploadReadTimeoutSeconds : 1800;
    }

    /**
     * 规范化后的 Base URL（去掉末尾 /）
     *
     * @return Base URL
     */
    public String resolvedBaseUrl() {
        if (baseUrl.isBlank()) {
            return "http://placeholder-oa-host";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
