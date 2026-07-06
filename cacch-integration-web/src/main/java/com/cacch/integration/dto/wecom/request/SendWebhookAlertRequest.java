package com.cacch.integration.dto.wecom.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 手动触发 Webhook 告警请求（平台级）
 * @author hongfu_zhou@cacch.com
 */
@Data
public class SendWebhookAlertRequest {

    /**
     * 业务域，如 meeting、apikey；测试时可填 platform
     */
    @NotBlank
    private String biz = "platform";

    /**
     * 告警标题
     */
    private String title = "手动测试告警";

    /**
     * 告警对象（任务名、模块名等）
     */
    private String subject = "Webhook 连通性测试";

    /**
     * 附加上下文
     */
    private String context;

    /**
     * 模拟错误信息
     */
    private String errorMessage = "这是一条手动触发的 Webhook 测试告警，请忽略";

    /**
     * 是否 @ 配置中的手机号
     */
    private Boolean mention = false;

    /**
     * 是否跳过去重，手动测试建议 true
     */
    private Boolean skipDedup = true;
}
