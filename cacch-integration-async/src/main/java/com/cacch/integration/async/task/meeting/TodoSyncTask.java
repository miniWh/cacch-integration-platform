package com.cacch.integration.async.task.meeting;

import com.cacch.integration.manager.meeting.api.IMeetingSyncManager;
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

    private final IMeetingSyncManager meetingSyncManager;

    @Scheduled(cron = "${meeting.sync.todo-cron:0 */15 * * * ?}")
    public void syncTodos() {
        log.info("【MeetingTask】开始执行待办回写");
        try {
            meetingSyncManager.syncTodosToSheet();
        } catch (Exception e) {
            log.error("【MeetingTask】待办回写失败", e);
        }
    }
}
