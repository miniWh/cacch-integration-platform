package com.cacch.integration.async.crm.task;

import com.cacch.integration.async.support.ScheduledTaskGuard;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.manager.crm.api.ICrmOrderCollectManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CRM 订单采集定时任务（阶段一）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmOrderCollectTask {

    private static final String BIZ = "crm";
    private static final String TASK_NAME = "CRM订单采集入库";

    private final ICrmOrderCollectManager crmOrderCollectManager;
    private final IWeComWebhookManager weComWebhookManager;
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 定时按当天 modify_time 采集 CRM 订单并拉取明细。
     *
     * <p>触发频率由 {@code crm.collect.cron} 配置，默认每 5 分钟；
     * 上一轮未结束则跳过；异常时 Webhook 告警。</p>
     */
    @Scheduled(cron = "${crm.collect.cron:0 */5 * * * ?}")
    public void collectOrders() {
        ScheduledTaskGuard.runExclusive(TASK_NAME, running, () -> {
            log.info("【CrmTask】开始执行{}", TASK_NAME);
            try {
                crmOrderCollectManager.collectToday();
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
