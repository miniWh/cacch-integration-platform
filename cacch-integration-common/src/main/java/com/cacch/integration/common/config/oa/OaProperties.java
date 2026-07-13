package com.cacch.integration.common.config.oa;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 致远 OA REST 配置属性
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "oa")
public class OaProperties {

    /**
     * OA 服务根地址，如 {@code http://oa.example.com}（不含末尾斜杠）
     */
    private String baseUrl = "http://10.80.64.18:900";

    /**
     * REST 账号用户名（path 中的 userName）
     */
    private String restUserName = "zhouhufu";

    /**
     * REST 账号密码（path 中的 password，禁止写入日志）
     */
    private String restPassword = "1a884fa3-affe-4533-8e1d-3af3c162361e";

    /**
     * 默认绑定登录名（Token 的 loginName）；为空则不带 loginName 参数
     */
    private String defaultLoginName;

    /**
     * Token Redis 缓存秒数；致远默认约 15 分钟，建议略小于该值
     */
    private long tokenTtlSeconds = 840L;

    /**
     * 默认表单模板编号（发起流程时可被请求覆盖）
     */
    private String templateCode = "CRM_ZYXS_001";

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
