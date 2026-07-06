package com.cacch.integration.manager.wecom.api;

import com.cacch.integration.common.dto.wecom.WeComAlertCommand;

/**
 * 企微 Webhook 平台级编排接口 — 供各业务域发送告警通知
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComWebhookManager {

    /**
     * 发送平台告警（Markdown，可选 @ 文本消息）
     *
     * @param command 告警命令（标题、上下文、去重键等）
     */
    void sendAlert(WeComAlertCommand command);

    /**
     * 手动触发测试告警（用于验证 Webhook 配置，默认跳过去重）
     *
     * @param command 测试告警命令
     */
    void sendManualTest(WeComAlertCommand command);
}
