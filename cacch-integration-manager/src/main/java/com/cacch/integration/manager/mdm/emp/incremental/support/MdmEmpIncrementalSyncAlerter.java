package com.cacch.integration.manager.mdm.emp.incremental.support;

import com.cacch.integration.common.dto.batch.BatchProcessDetail;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.dto.batch.BatchProcessStatus;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 人员增量同步企微 Webhook 告警
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
@RequiredArgsConstructor
public class MdmEmpIncrementalSyncAlerter {

    private static final String WEBHOOK_BIZ = "人员主数据";
    private static final String DEDUP_TYPE_PULL = "人员主数据拉取";
    private static final String DEDUP_TYPE_PROCESS = "人员主数据处理";
    private static final DateTimeFormatter ALERT_HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final int PROCESS_FAILURE_SAMPLE_LIMIT = 5;

    private final IWeComWebhookManager weComWebhookManager;

    /**
     * Pull 阶段整轮异常
     *
     * @param errorMessage 错误摘要
     * @param error        原始异常，可为 null
     */
    public void alertPullStage(String errorMessage, Throwable error) {
        send(WebhookAlertParams.pullStage(errorMessage, error));
    }

    /**
     * Process 阶段整轮异常
     *
     * @param errorMessage 错误摘要
     * @param error        原始异常，可为 null
     * @param limit        认领上限
     */
    public void alertProcessStage(String errorMessage, Throwable error, int limit) {
        send(WebhookAlertParams.processStage(errorMessage, error, limit));
    }

    /**
     * Process 阶段单批部分员工失败
     *
     * @param result 本轮处理统计
     * @param limit  认领上限
     */
    public void alertProcessPartialFailure(BatchProcessResult<String> result, int limit) {
        send(WebhookAlertParams.processPartialFailure(result, limit));
    }

    private void send(WebhookAlertParams alert) {
        String hourKey = LocalDateTime.now().format(ALERT_HOUR_FMT);
        String dedupId = StringUtils.hasText(alert.dedupIdSuffix())
                ? hourKey + alert.dedupIdSuffix()
                : hourKey;
        weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                .biz(WEBHOOK_BIZ)
                .title(alert.title())
                .subject(alert.subject())
                .context(alert.context())
                .detail(alert.detail())
                .errorMessage(alert.errorMessage())
                .error(alert.error())
                .dedupType(alert.dedupType())
                .dedupId(dedupId)
                .mention(true)
                .build());
    }

    private record WebhookAlertParams(
            String title,
            String subject,
            String context,
            String detail,
            String errorMessage,
            Throwable error,
            String dedupType,
            String dedupIdSuffix
    ) {

        static WebhookAlertParams pullStage(String errorMessage, Throwable error) {
            return new WebhookAlertParams(
                    "人员增量 Pull 异常",
                    "iHR增量Pull入队",
                    "从 iHR 拉取 additions/modifications 入队 stg_staff_record 失败",
                    null,
                    errorMessage,
                    error,
                    DEDUP_TYPE_PULL,
                    null);
        }

        static WebhookAlertParams processStage(String errorMessage, Throwable error, int limit) {
            return new WebhookAlertParams(
                    "人员增量 Process 异常",
                    "iHR增量Process落库",
                    "limit=" + limit + "，从 stg_staff_record 认领并落库整轮终止",
                    null,
                    errorMessage,
                    error,
                    DEDUP_TYPE_PROCESS,
                    ":stage");
        }

        static WebhookAlertParams processPartialFailure(BatchProcessResult<String> result, int limit) {
            String sample = formatFailureSample(result);
            String detail = String.format("总数=%d, 成功=%d, 跳过=%d, 失败=%d%s",
                    result.getTotalCount(), result.getSuccessCount(), result.getSkipCount(),
                    result.getFailureCount(), sample);
            return new WebhookAlertParams(
                    "人员增量 Process 部分失败",
                    "iHR增量Process落库",
                    "limit=" + limit + "，本轮存在单员工拉取或落库失败",
                    detail,
                    "失败数=" + result.getFailureCount(),
                    null,
                    DEDUP_TYPE_PROCESS,
                    ":partial:" + result.getFailureCount());
        }

        private static String formatFailureSample(BatchProcessResult<String> result) {
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (BatchProcessDetail<String> d : result.getDetails()) {
                if (d.getStatus() != BatchProcessStatus.FAILURE) {
                    continue;
                }
                if (shown == 0) {
                    sb.append("；样例: ");
                } else if (shown >= PROCESS_FAILURE_SAMPLE_LIMIT) {
                    sb.append("…");
                    break;
                }
                if (shown > 0) {
                    sb.append("; ");
                }
                sb.append(d.getItem());
                if (StringUtils.hasText(d.getReason())) {
                    sb.append("(").append(d.getReason()).append(")");
                }
                shown++;
            }
            if (result.isDetailsTruncated()) {
                sb.append("（明细已截断）");
            }
            return sb.toString();
        }
    }
}