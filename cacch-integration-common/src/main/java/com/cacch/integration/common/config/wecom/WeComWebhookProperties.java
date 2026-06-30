package com.cacch.integration.common.config.wecom;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * 企微群机器人 Webhook 配置 — 用于同步异常告警等通知
 *
 * <p>在企微群聊 → 群机器人 → 添加机器人 → 复制 Webhook 地址中的 key 填入 {@code wecom.webhook.key}。</p>
 */
@ConfigurationProperties(prefix = "wecom.webhook")
public class WeComWebhookProperties {

    /**
     * 是否启用 Webhook 告警
     */
    private boolean enabled = false;

    /**
     * 群机器人 Webhook Key（URL 中 key= 后面的部分）
     */
    private String key = "";

    /**
     * 告警 @ 的手机号列表（可选）
     */
    private List<String> mentionMobiles = List.of();

    /**
     * 相同告警去重间隔（秒），避免定时任务反复失败时刷屏
     */
    private long alertIntervalSeconds = 1800L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getMentionMobiles() {
        return mentionMobiles != null ? Collections.unmodifiableList(mentionMobiles) : List.of();
    }

    public void setMentionMobiles(List<String> mentionMobiles) {
        this.mentionMobiles = mentionMobiles;
    }

    public long getAlertIntervalSeconds() {
        return alertIntervalSeconds;
    }

    public void setAlertIntervalSeconds(long alertIntervalSeconds) {
        this.alertIntervalSeconds = alertIntervalSeconds;
    }

    /**
     * 配置是否可用于发送（enabled 且 key 非空）
     */
    public boolean isReady() {
        return enabled && key != null && !key.isBlank();
    }
}
