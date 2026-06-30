package com.cacch.integration.integration.wecom.client.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * 企微 Webhook — Markdown 消息请求
 */
@Data
@Builder
public class WeComWebhookMarkdownRequest {

    private String msgtype;

    private MarkdownBody markdown;

    @Data
    @Builder
    public static class MarkdownBody {

        private String content;
    }

    public static WeComWebhookMarkdownRequest of(String content) {
        return WeComWebhookMarkdownRequest.builder()
                .msgtype("markdown")
                .markdown(MarkdownBody.builder().content(content).build())
                .build();
    }
}
