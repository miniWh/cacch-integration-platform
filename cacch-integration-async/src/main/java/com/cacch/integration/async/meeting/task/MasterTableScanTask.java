package com.cacch.integration.async.meeting.task;

import com.cacch.integration.async.support.ScheduledTaskGuard;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.manager.meeting.api.IMeetingSyncManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 总控表扫描定时任务 — 扫描已批准申请并创建员工会议管理智能表格
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterTableScanTask {

    private static final String BIZ = "meeting";
    private static final String TASK_NAME = "总控表扫描";

    private final IMeetingSyncManager meetingSyncManager;
    private final IWeComWebhookManager weComWebhookManager;
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 定时扫描总控表并创建员工会议管理智能表格。
     *
     * <p>触发频率由 {@code meeting.sync.master-cron} 配置，默认每 3 分钟；
     * 上一轮未结束则跳过本次；异常时发送 Webhook 告警，不向上抛出以免影响调度线程。</p>
     */
    @Scheduled(cron = "${meeting.sync.master-cron:0 */3 * * * ?}")
    public void scanMasterTable() {
        ScheduledTaskGuard.runExclusive(TASK_NAME, running, () -> {
            log.info("【MeetingTask】开始执行{}", TASK_NAME);
            try {
                meetingSyncManager.scanMasterAndProvision();
            } catch (Exception e) {
                log.info("【MeetingTask】{}异常终止, reason={}", TASK_NAME, e.getMessage());
                log.error("【MeetingTask】{}失败", TASK_NAME, e);
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
