package com.cacch.integration.async.task.meeting;

import com.cacch.integration.async.support.ScheduledTaskTraceSupport;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.manager.meeting.api.IMeetingSyncManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待办回写智能表格定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoSyncTask {

    private static final String BIZ = "meeting";
    private static final String TASK_NAME = "待办回写";

    private final IMeetingSyncManager meetingSyncManager;
    private final IWeComWebhookManager weComWebhookManager;

    @Scheduled(cron = "${meeting.sync.todo-cron:0 */15 * * * ?}")
    public void syncTodos() {
        ScheduledTaskTraceSupport.runWithTraceId(() -> {
            log.info("【MeetingTask】开始执行{}", TASK_NAME);
            try {
                meetingSyncManager.syncTodosToSheet();
            } catch (Exception e) {
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
