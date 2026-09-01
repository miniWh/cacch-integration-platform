package com.cacch.integration.common.config.wecom;

import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * 企业微信单个应用配置 — 由 yml 的 wecom.apps 列表绑定
 *
 * <pre>
 * 一个 (corpid, app-key) 对应一个独立的 token 缓存槽位。
 * 同一公司可配置多个 app-key（如通讯录、客户联系），各自使用不同 secret。
 *
 * <strong>绑定陷阱警示（2026-09-01 测试环境事故根因）</strong>：若环境变量命名形如
 * {@code WECOM_APPS_0_SECRET}，Spring 会将其映射为 {@code wecom.apps[0].secret}；
 * 由于 SystemEnvironment 优先级高于 yml，List 绑定会整体改从环境变量取值，
 * yml 中的 corpid/app-key 被忽略 → 得到 corpid=null 的实例（列表 size 正常，
 * 看似绑定成功实则失败）。环境变量必须用语义化命名（如 WECOM_SELF_BUILT_SECRET），
 * 仅经 yml 占位符 ${} 引用。
 * </pre>
 *
 * @author hongfu_zhou@cacch.com
 */
public class WeComAppConfig {

    /**
     * 企业 ID，注册企微时分配
     */
    private final String corpid;

    /**
     * 业务标识，对应一类企微 API
     * 建议值：address-book（通讯录）、customer-contact（客户联系）、calendar（日程）
     */
    private final String appKey;

    /**
     * 该企业下该应用的凭证密钥
     */
    private final String secret;

    @ConstructorBinding
    public WeComAppConfig(String corpid, String appKey, String secret) {
        this.corpid = corpid;
        this.appKey = appKey;
        this.secret = secret;
    }

    public String getCorpid() {
        return corpid;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getSecret() {
        return secret;
    }
}
