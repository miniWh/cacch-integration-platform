package com.cacch.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 企业微信多应用配置属性 — 由 yml 的 wecom.apps 列表注入
 *
 * <pre>
 * 一个 (corpid, app-key) 唯一标识一个 token 缓存槽位。
 * 同一公司可配置多个 app-key（通讯录、客户联系等），各自使用不同 secret。
 * </pre>
 *
 * @author cacch-integration
 */
@Data
@Component
@ConfigurationProperties(prefix = "wecom")
public class WeComProperties {

    /**
     * 应用配置列表（由 yml / env 注入）
     */
    private List<WeComAppConfig> apps;

    /**
     * 根据企业 ID 和业务标识查找对应配置
     *
     * @param corpid 企业 ID
     * @param appKey 业务标识
     * @return 匹配的配置
     */
    public Optional<WeComAppConfig> findByCorpidAndAppKey(String corpid, String appKey) {
        if (apps == null) {
            return Optional.empty();
        }
        return apps.stream()
                .filter(a -> corpid.equals(a.getCorpid()) && appKey.equals(a.getAppKey()))
                .findFirst();
    }

    /**
     * 获取所有应用配置（不可变列表）
     *
     * @return 全部应用配置，未配置时返回空列表
     */
    public List<WeComAppConfig> getAllApps() {
        if (apps == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(apps);
    }
}
