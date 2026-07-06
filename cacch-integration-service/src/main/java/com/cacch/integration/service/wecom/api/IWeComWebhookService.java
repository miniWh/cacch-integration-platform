package com.cacch.integration.service.wecom.api;

import java.util.List;

/**
 * 企微 Webhook 通知服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComWebhookService {

    /**
     * 发送 Markdown 消息（带告警去重）
     *
     * @param dedupKey         去重键，相同键在配置的间隔内只发送一次；传 null 则不去重
     * @param markdownContent  Markdown 正文
     */
    void sendMarkdown(String dedupKey, String markdownContent);

    /**
     * 发送文本消息并 @ 指定手机号
     *
     * @param dedupKey        去重键，传 null 则不去重
     * @param textContent     文本正文
     * @param mentionMobiles  需 @ 的手机号列表，可为空
     */
    void sendText(String dedupKey, String textContent, List<String> mentionMobiles);
}
