package com.cacch.integration.integration.wecom.client.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 企微 Webhook — 文本消息请求（支持 @ 指定成员）
 */
@Data
@Builder
public class WeComWebhookTextRequest {

    private String msgtype;

    private TextBody text;

    @Data
    @Builder
    public static class TextBody {

        private String content;

        @JsonProperty("mentioned_mobile_list")
        private List<String> mentionedMobileList;
    }

    public static WeComWebhookTextRequest of(String content, List<String> mentionMobiles) {
        return WeComWebhookTextRequest.builder()
                .msgtype("text")
                .text(TextBody.builder()
                        .content(content)
                        .mentionedMobileList(mentionMobiles)
                        .build())
                .build();
    }
}
