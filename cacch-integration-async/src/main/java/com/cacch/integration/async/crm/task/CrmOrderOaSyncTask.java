package com.cacch.integration.async.crm.task;

import com.cacch.integration.async.support.ScheduledTaskGuard;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.manager.crm.api.ICrmOrderOaSyncManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CRM 订单 OA 表单同步定时任务（阶段二）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmOrderOaSyncTask {

    private static final String BIZ = "crm";
    private static final String TASK_NAME = "CRM订单OA表单同步";

    private final ICrmOrderOaSyncManager crmOrderOaSyncManager;
    private final IWeComWebhookManager weComWebhookManager;
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 定时扫描 PENDING/RETRY 明细并发起 OA 表单。
     *
     * <p>触发频率由 {@code crm.sync.cron} 配置，默认每 5 分钟；
     * 上一轮未结束则跳过；异常时 Webhook 告警。</p>
     */
    @Scheduled(cron = "${crm.sync.cron:0 */5 * * * ?}")
    public void syncOrdersToOa() {
        ScheduledTaskGuard.runExclusive(TASK_NAME, running, () -> {
            log.info("【CrmTask】开始执行{}", TASK_NAME);
            try {
                crmOrderOaSyncManager.syncPendingDetails();
            } catch (Exception e) {
                log.info("【CrmTask】{}异常终止, reason={}", TASK_NAME, e.getMessage());
                log.error("【CrmTask】{}失败", TASK_NAME, e);
                weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                        .biz(BIZ)
                        .title("定时同步任务异常")
                        .subject(TASK_NAME)
                        .error(e)
                        .dedupType("task")
                        .dedupId(TASK_NAME)
                        .mention(true)
                        .build());
            }
        });
    }
}
