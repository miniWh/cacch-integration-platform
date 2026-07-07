package com.cacch.integration.service.wecom.api.impl;

import com.cacch.integration.common.config.wecom.WeComWebhookProperties;
import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.wecom.client.WeComWebhookClient;
import com.cacch.integration.integration.wecom.client.dto.webhook.WeComWebhookMarkdownRequest;
import com.cacch.integration.integration.wecom.client.dto.webhook.WeComWebhookResponse;
import com.cacch.integration.integration.wecom.client.dto.webhook.WeComWebhookTextRequest;
import com.cacch.integration.service.wecom.api.IWeComWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 企微 Webhook 通知服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComWebhookServiceImpl implements IWeComWebhookService {

    private final WeComWebhookClient weComWebhookClient;
    private final WeComWebhookProperties webhookProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void sendMarkdown(String dedupKey, String markdownContent) {
        if (!webhookProperties.isReady()) {
            log.info("【WeComWebhook】未启用或未配置 key，跳过发送");
            return;
        }
        if (isDuplicate(dedupKey)) {
            log.info("【WeComWebhook】告警去重命中, dedupKey={}", dedupKey);
            return;
        }
        WeComWebhookResponse response = weComWebhookClient.sendMarkdown(
                webhookProperties.getKey(),
                WeComWebhookMarkdownRequest.of(markdownContent));
        if (!response.isSuccess()) {
            log.error("【WeComWebhook】发送失败, errcode={}, errmsg={}",
                    response.getErrCode(), response.getErrMsg());
            return;
        }
        markSent(dedupKey);
        log.info("【WeComWebhook】Markdown 消息发送成功");
    }

    @Override
    public void sendText(String dedupKey, String textContent, List<String> mentionMobiles) {
        if (!webhookProperties.isReady()) {
            log.info("【WeComWebhook】未启用或未配置 key，跳过发送");
            return;
        }
        if (isDuplicate(dedupKey)) {
            log.info("【WeComWebhook】告警去重命中, dedupKey={}", dedupKey);
            return;
        }
        WeComWebhookResponse response = weComWebhookClient.sendText(
                webhookProperties.getKey(),
                WeComWebhookTextRequest.of(textContent, mentionMobiles));
        if (!response.isSuccess()) {
            log.error("【WeComWebhook】发送失败, errcode={}, errmsg={}",
                    response.getErrCode(), response.getErrMsg());
            return;
        }
        markSent(dedupKey);
        log.info("【WeComWebhook】文本消息发送成功");
    }

    private boolean isDuplicate(String dedupKey) {
        if (dedupKey == null || dedupKey.isBlank()) {
            return false;
        }
        String redisKey = WeComConstants.webhookAlertRedisKey(dedupKey);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey));
    }

    private void markSent(String dedupKey) {
        if (dedupKey == null || dedupKey.isBlank()) {
            return;
        }
        String redisKey = WeComConstants.webhookAlertRedisKey(dedupKey);
        long ttl = Math.max(webhookProperties.getAlertIntervalSeconds(), 60L);
        stringRedisTemplate.opsForValue().set(redisKey, "1", Duration.ofSeconds(ttl));
    }
}
