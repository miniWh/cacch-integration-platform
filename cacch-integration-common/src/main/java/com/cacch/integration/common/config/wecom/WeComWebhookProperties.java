package com.cacch.integration.common.config.wecom;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 企微群机器人 Webhook 配置 — 用于同步异常告警等通知
 *
 * <p>在企微群聊 → 群机器人 → 添加机器人 → 复制 Webhook 地址中的 key 填入 {@code wecom.webhook.key}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@ConfigurationProperties(prefix = "wecom.webhook")
public class WeComWebhookProperties {

    private final boolean enabled;
    private final String key;
    private final List<String> mentionMobiles;
    private final long alertIntervalSeconds;

    public WeComWebhookProperties(boolean enabled, String key, List<String> mentionMobiles,
                                  long alertIntervalSeconds) {
        this.enabled = enabled;
        this.key = key != null ? key : "";
        this.mentionMobiles = mentionMobiles != null ? List.copyOf(mentionMobiles) : List.of();
        this.alertIntervalSeconds = alertIntervalSeconds > 0 ? alertIntervalSeconds : 1800L;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getKey() {
        return key;
    }

    public List<String> getMentionMobiles() {
        return mentionMobiles;
    }

    public long getAlertIntervalSeconds() {
        return alertIntervalSeconds;
    }

    /**
     * 配置是否可用于发送（enabled 且 key 非空）
     *
     * @return 是否可发送 Webhook 消息
     */
    public boolean isReady() {
        return enabled && !key.isBlank();
    }
}
