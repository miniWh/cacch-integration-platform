package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.wecom.client.dto.webhook.WeComWebhookMarkdownRequest;
import com.cacch.integration.integration.wecom.client.dto.webhook.WeComWebhookResponse;
import com.cacch.integration.integration.wecom.client.dto.webhook.WeComWebhookTextRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 企微群机器人 Webhook HTTP 客户端
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComWebhookClient {

    private final RestTemplate restTemplate;

    public WeComWebhookResponse sendMarkdown(String webhookKey, WeComWebhookMarkdownRequest request) {
        String url = String.format(WeComConstants.WEBHOOK_SEND_URL, webhookKey);
        log.info("【WeComWebhook】发送 Markdown 消息");
        return post(url, request);
    }

    public WeComWebhookResponse sendText(String webhookKey, WeComWebhookTextRequest request) {
        String url = String.format(WeComConstants.WEBHOOK_SEND_URL, webhookKey);
        log.info("【WeComWebhook】发送文本消息");
        return post(url, request);
    }

    private WeComWebhookResponse post(String url, Object request) {
        try {
            WeComWebhookResponse response = restTemplate.postForObject(url, request, WeComWebhookResponse.class);
            if (response == null) {
                log.error("【WeComWebhook】发送消息返回 null");
                throw new RestClientException("企微 Webhook 返回 null");
            }
            return response;
        } catch (RestClientException e) {
            log.error("【WeComWebhook】HTTP 调用失败", e);
            throw e;
        }
    }
}
