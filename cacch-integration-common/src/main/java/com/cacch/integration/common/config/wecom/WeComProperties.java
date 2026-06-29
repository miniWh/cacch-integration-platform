package com.cacch.integration.common.config.wecom;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 企业微信多应用配置属性 — 由 yml 的 wecom.apps 列表注入
 *
 * <p>配置 POJO 定义在 common 模块，由 web 模块通过 {@code @EnableConfigurationProperties} 注册为 Bean。</p>
 *
 * @author cacch-integration
 */
@ConfigurationProperties(prefix = "wecom")
public class WeComProperties {

    private final List<WeComAppConfig> apps;

    public WeComProperties(List<WeComAppConfig> apps) {
        this.apps = apps != null ? List.copyOf(apps) : List.of();
    }

    /**
     * 根据企业 ID 和业务标识查找对应配置
     */
    public Optional<WeComAppConfig> findByCorpidAndAppKey(String corpid, String appKey) {
        return apps.stream()
                .filter(a -> corpid.equals(a.getCorpid()) && appKey.equals(a.getAppKey()))
                .findFirst();
    }

    /**
     * 获取所有应用配置（不可变列表）
     */
    public List<WeComAppConfig> getAllApps() {
        return Collections.unmodifiableList(apps);
    }

    /**
     * 获取自建应用配置：优先匹配 {@link WeComConstants#SELF_BUILT_APP_KEY}，否则取首个配置
     */
    public Optional<WeComAppConfig> findSelfBuiltApp() {
        Optional<WeComAppConfig> byKey = apps.stream()
                .filter(a -> WeComConstants.SELF_BUILT_APP_KEY.equals(a.getAppKey()))
                .findFirst();
        if (byKey.isPresent()) {
            return byKey;
        }
        return apps.isEmpty() ? Optional.empty() : Optional.of(apps.getFirst());
    }
}
