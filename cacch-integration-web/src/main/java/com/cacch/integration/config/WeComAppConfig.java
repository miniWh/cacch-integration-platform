package com.cacch.integration.config;

import lombok.Data;

/**
 * 企业微信单个应用配置 — 由 yml 的 wecom.apps 列表绑定
 *
 * <pre>
 * 一个 (corpid, app-key) 对应一个独立的 token 缓存槽位。
 * 同一公司可配置多个 app-key（如通讯录、客户联系），各自使用不同 secret。
 * </pre>
 *
 * @author cacch-integration
 */
@Data
public class WeComAppConfig {

    /**
     * 企业 ID，注册企微时分配
     */
    private String corpid;

    /**
     * 业务标识，对应一类企微 API
     * 建议值：address-book（通讯录）、customer-contact（客户联系）、calendar（日程）
     */
    private String appKey;

    /**
     * 该企业下该应用的凭证密钥
     */
    private String secret;
}
