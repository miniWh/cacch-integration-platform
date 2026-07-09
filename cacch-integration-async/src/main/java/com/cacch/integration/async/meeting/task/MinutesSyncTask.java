package com.cacch.integration.async.meeting.task;

import com.cacch.integration.async.support.ScheduledTaskTraceSupport;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.manager.meeting.api.IMeetingSyncManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 会议纪要拉取与待办解析定时任务
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinutesSyncTask {

    private static final String BIZ = "meeting";
    private static final String TASK_NAME = "纪要拉取与待办解析";

    private final IMeetingSyncManager meetingSyncManager;
    private final IWeComWebhookManager weComWebhookManager;

    /**
     * 定时拉取已结束会议的腾讯会议智能纪要并解析待办。
     *
     * <p>触发频率由 {@code meeting.sync.minutes-cron} 配置，默认每 3 分钟；
     * 异常时发送 Webhook 告警，不向上抛出以免影响调度线程。</p>
     */
    @Scheduled(cron = "${meeting.sync.minutes-cron:0 */3 * * * ?}")
    public void syncMinutes() {
        ScheduledTaskTraceSupport.runWithTraceId(() -> {
            log.info("【MeetingTask】开始执行{}", TASK_NAME);
            try {
                meetingSyncManager.syncMeetingMinutesFromWeCom();
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
