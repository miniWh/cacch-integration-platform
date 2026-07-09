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
 * 待办回写智能表格定时任务
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoSyncTask {

    private static final String BIZ = "meeting";
    private static final String TASK_NAME = "待办回写";

    private final IMeetingSyncManager meetingSyncManager;
    private final IWeComWebhookManager weComWebhookManager;

    /**
     * 定时将待办事项回写到智能表格子表
     */
    @Scheduled(cron = "${meeting.sync.todo-cron:0 */3 * * * ?}")
    public void syncTodos() {
        ScheduledTaskTraceSupport.runWithTraceId(() -> {
            log.info("【MeetingTask】开始执行{}", TASK_NAME);
            try {
                meetingSyncManager.syncTodosToSheet();
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
