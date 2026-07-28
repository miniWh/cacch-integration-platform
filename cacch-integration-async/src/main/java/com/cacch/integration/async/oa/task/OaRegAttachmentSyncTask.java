package com.cacch.integration.async.oa.task;

import com.cacch.integration.async.support.ScheduledTaskGuard;
import com.cacch.integration.common.config.oa.OaRegAttachmentSyncProperties;
import com.cacch.integration.common.constant.oa.OaRegReportConstants;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.manager.oa.api.IOaRegAttachmentSyncManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 国内登记报告资料列表附件同步定时任务
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "oa.attachment-sync", name = "enabled", havingValue = "true")
public class OaRegAttachmentSyncTask {

    private static final String BIZ = "oa";
    private static final String TASK_NAME = "国内登记报告附件同步";

    private final IOaRegAttachmentSyncManager syncManager;
    private final IWeComWebhookManager weComWebhookManager;
    private final OaRegAttachmentSyncProperties syncProperties;
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 定时扫描 OA 资料列表并同步共享盘附件至 OA。
     *
     * <p>触发频率由 {@code oa.attachment-sync.cron} 配置；上一轮未结束则跳过。</p>
     */
    @Scheduled(cron = "${oa.attachment-sync.cron:0 */10 * * * ?}")
    public void syncRegReportAttachments() {
        ScheduledTaskGuard.runExclusive(TASK_NAME, running, () -> {
            log.info("【OaTask】开始执行{}, enabled={}, batchSize={}",
                    TASK_NAME, syncProperties.isEnabled(), syncProperties.getBatchSize());
            try {
                syncManager.syncAttachments(null);
            } catch (Exception e) {
                log.info("【OaTask】{}异常终止, reason={}", TASK_NAME, e.getMessage());
                log.error("【OaTask】{}失败", TASK_NAME, e);
                weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                        .biz(BIZ)
                        .title("定时同步任务异常")
                        .subject(TASK_NAME)
                        .error(e)
                        .dedupType("task")
                        .dedupId(OaRegReportConstants.LOG_BIZ)
                        .mention(true)
                        .build());
            }
        });
    }
}
