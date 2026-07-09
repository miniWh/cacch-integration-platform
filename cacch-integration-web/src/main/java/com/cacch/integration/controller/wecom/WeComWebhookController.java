package com.cacch.integration.controller.wecom;

import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.wecom.request.SendWebhookAlertRequest;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企微 Webhook 平台级接口 — 各业务域告警通知的统一入口
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/wecom/webhook")
@RequiredArgsConstructor
public class WeComWebhookController {

    private final IWeComWebhookManager weComWebhookManager;

    /**
     * 手动触发 Webhook 测试告警（验证群机器人配置）
     *
     * <p>请求体可省略，省略时使用默认测试内容。</p>
     *
     * @param request 告警请求体，可为空；为空时使用默认字段
     * @return 统一成功结果，data 为 null
     */
    @PostMapping("/test")
    public Result<Void> sendTestAlert(@RequestBody(required = false) @Valid SendWebhookAlertRequest request) {
        SendWebhookAlertRequest req = request != null ? request : new SendWebhookAlertRequest();
        weComWebhookManager.sendManualTest(toCommand(req));
        return Result.success(null);
    }

    /**
     * 发送自定义 Webhook 告警（供运维或联调使用）
     *
     * @param request 告警请求体，不可为空
     * @return 统一成功结果，data 为 null
     */
    @PostMapping("/alert")
    public Result<Void> sendAlert(@Valid @RequestBody SendWebhookAlertRequest request) {
        WeComAlertCommand command = toCommand(request);
        boolean skipDedup = request.getSkipDedup() != null && request.getSkipDedup();
        WeComAlertCommand alertCommand = WeComAlertCommand.builder()
                .biz(command.getBiz())
                .title(command.getTitle())
                .subject(command.getSubject())
                .context(command.getContext())
                .errorMessage(command.getErrorMessage())
                .skipDedup(skipDedup)
                .dedupType(skipDedup ? null : "manual")
                .dedupId(command.getSubject())
                .mention(Boolean.TRUE.equals(request.getMention()))
                .build();
        weComWebhookManager.sendAlert(alertCommand);
        return Result.success(null);
    }

    private WeComAlertCommand toCommand(SendWebhookAlertRequest request) {
        return WeComAlertCommand.builder()
                .biz(request.getBiz())
                .title(request.getTitle())
                .subject(request.getSubject())
                .context(request.getContext())
                .errorMessage(request.getErrorMessage())
                .mention(Boolean.TRUE.equals(request.getMention()))
                .build();
    }
}
