package com.cacch.integration.manager.wecom.api.impl;

import com.cacch.integration.common.config.wecom.WeComWebhookProperties;
import com.cacch.integration.common.constant.trace.TraceConstants;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import com.cacch.integration.service.wecom.api.IWeComWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * 企微 Webhook 平台级编排实现
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComWebhookManagerImpl implements IWeComWebhookManager {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_ERROR_LENGTH = 500;

    private final IWeComWebhookService weComWebhookService;
    private final WeComWebhookProperties webhookProperties;

    @Value("${spring.profiles.active:unknown}")
    private String activeProfile;

    @Override
    public void sendAlert(WeComAlertCommand command) {
        if (!webhookProperties.isReady()) {
            log.info("【WeComWebhook】未启用，跳过告警, biz={}, title={}", command.getBiz(), command.getTitle());
            return;
        }
        String dedupKey = resolveDedupKey(command);
        String markdown = buildMarkdown(command);
        weComWebhookService.sendMarkdown(dedupKey, markdown);
        if (command.isMention()) {
            sendMentionText(command, dedupKey);
        }
        log.info("【WeComWebhook】平台告警已发送, biz={}, title={}", command.getBiz(), command.getTitle());
    }

    @Override
    public void sendManualTest(WeComAlertCommand command) {
        assertWebhookReady();
        WeComAlertCommand testCommand = WeComAlertCommand.builder()
                .biz(command.getBiz())
                .title(StringUtils.hasText(command.getTitle()) ? command.getTitle() : "手动测试告警")
                .subject(command.getSubject())
                .context(command.getContext())
                .detail(command.getDetail())
                .errorMessage(command.getErrorMessage())
                .error(command.getError())
                .skipDedup(true)
                .mention(command.isMention())
                .build();
        sendAlert(testCommand);
        log.info("【WeComWebhook】手动测试告警已触发, biz={}", command.getBiz());
    }

    private void assertWebhookReady() {
        if (!webhookProperties.isReady()) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "企微 Webhook 未启用或未配置 key，请在 wecom.webhook 中配置");
        }
    }

    private void sendMentionText(WeComAlertCommand command, String dedupKey) {
        if (webhookProperties.getMentionMobiles().isEmpty()) {
            log.info("【WeComWebhook】跳过 @提醒, reason=未配置 mentionMobiles");
            return;
        }
        String textDedupKey = command.isSkipDedup() ? null : dedupKey + ":text";
        String prefix = StringUtils.hasText(command.getBiz()) ? command.getBiz() : "platform";
        String text = String.format("[%s告警] %s: %s", prefix, command.getSubject(),
                truncate(resolveErrorMessage(command), 200));
        weComWebhookService.sendText(textDedupKey, text, webhookProperties.getMentionMobiles());
    }

    private String resolveDedupKey(WeComAlertCommand command) {
        if (command.isSkipDedup()) {
            return null;
        }
        if (!StringUtils.hasText(command.getDedupType())) {
            return null;
        }
        String dedupId = StringUtils.hasText(command.getDedupId()) ? command.getDedupId() : command.getSubject();
        return buildDedupKey(command.getBiz(), command.getDedupType(), dedupId, resolveErrorMessage(command));
    }

    private String buildMarkdown(WeComAlertCommand command) {
        String traceId = MDC.get(TraceConstants.MDC_TRACE_ID);
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(command.getTitle()).append("\n");
        if (StringUtils.hasText(command.getBiz())) {
            sb.append("> **业务域**: ").append(escapeMarkdown(command.getBiz())).append("\n");
        }
        sb.append("> **对象**: <font color=\"warning\">")
                .append(escapeMarkdown(command.getSubject())).append("</font>\n");
        sb.append("> **环境**: ").append(activeProfile).append("\n");
        sb.append("> **时间**: ").append(LocalDateTime.now().format(TIME_FMT)).append("\n");
        if (StringUtils.hasText(traceId)) {
            sb.append("> **TraceId**: ").append(traceId).append("\n");
        }
        if (StringUtils.hasText(command.getContext())) {
            sb.append("> **上下文**: ").append(escapeMarkdown(command.getContext())).append("\n");
        }
        if (StringUtils.hasText(command.getDetail())) {
            sb.append("\n").append(escapeMarkdown(command.getDetail())).append("\n");
        }
        sb.append("\n**错误信息**\n");
        sb.append("> ").append(escapeMarkdown(truncate(resolveErrorMessage(command), MAX_ERROR_LENGTH)));
        return sb.toString();
    }

    private String resolveErrorMessage(WeComAlertCommand command) {
        if (StringUtils.hasText(command.getErrorMessage())) {
            return command.getErrorMessage();
        }
        return rootMessage(command.getError());
    }

    private String rootMessage(Throwable e) {
        if (e == null) {
            return "未知错误";
        }
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\n", " ").replace("\r", "");
    }

    private String buildDedupKey(String biz, String type, String id, String message) {
        String raw = biz + ":" + type + ":" + id + ":" + message;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return biz + ":" + type + ":" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return biz + ":" + type + ":" + Math.abs(raw.hashCode());
        }
    }
}
