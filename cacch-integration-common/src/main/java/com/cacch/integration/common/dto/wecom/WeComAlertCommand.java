package com.cacch.integration.common.dto.wecom;

import lombok.Builder;
import lombok.Value;

/**
 * 平台级企微 Webhook 告警指令 — 各业务域统一使用此模型发送告警
 */
@Value
@Builder
public class WeComAlertCommand {

    /**
     * 业务域标识，如 meeting、apikey
     */
    String biz;

    /**
     * 告警标题，如「定时同步任务异常」
     */
    String title;

    /**
     * 告警主体（任务名、对象名等）
     */
    String subject;

    /**
     * 附加上下文（可选）
     */
    String context;

    /**
     * 补充说明（可选）
     */
    String detail;

    /**
     * 错误信息文本（与 error 二选一）
     */
    String errorMessage;

    /**
     * 异常对象（与 errorMessage 二选一）
     */
    Throwable error;

    /**
     * 去重类型前缀，如 task、table；为空则不 dedup
     */
    String dedupType;

    /**
     * 去重 ID，与 dedupType 配合使用
     */
    String dedupId;

    /**
     * 是否跳过去重
     */
    @Builder.Default
    boolean skipDedup = false;

    /**
     * 是否 @ 配置中的手机号
     */
    @Builder.Default
    boolean mention = false;
}
