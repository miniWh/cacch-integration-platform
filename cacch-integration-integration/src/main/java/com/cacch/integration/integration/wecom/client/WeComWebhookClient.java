package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
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

    private static final String BIZ = "WeComWebhook";

    private final RestTemplate restTemplate;

    public WeComWebhookResponse sendMarkdown(String webhookKey, WeComWebhookMarkdownRequest request) {
        String url = String.format(WeComConstants.WEBHOOK_SEND_URL, webhookKey);
        return post(url, request, "发送 Markdown 消息");
    }

    public WeComWebhookResponse sendText(String webhookKey, WeComWebhookTextRequest request) {
        String url = String.format(WeComConstants.WEBHOOK_SEND_URL, webhookKey);
        return post(url, request, "发送文本消息");
    }

    private WeComWebhookResponse post(String url, Object request, String action) {
        ThirdPartyHttpLogSupport.logRequest(BIZ, action, url, request);
        try {
            WeComWebhookResponse response = restTemplate.postForObject(url, request, WeComWebhookResponse.class);
            ThirdPartyHttpLogSupport.logResponse(BIZ, action, response);
            if (response == null) {
                log.info("【WeComWebhook】消息发送终止, reason=接口返回null");
                log.error("【WeComWebhook】发送消息返回 null");
                throw new RestClientException("企微 Webhook 返回 null");
            }
            return response;
        } catch (RestClientException e) {
            log.info("【WeComWebhook】消息发送终止, reason={}", e.getMessage());
            log.error("【WeComWebhook】HTTP 调用失败", e);
            throw e;
        }
    }
}
