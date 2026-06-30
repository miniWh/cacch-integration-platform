package com.cacch.integration.async.task.meeting;

import com.cacch.integration.manager.meeting.api.IMeetingSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 会议行同步与建会定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingSyncTask {

    private final IMeetingSyncManager meetingSyncManager;

    @Scheduled(cron = "${meeting.sync.meeting-cron:0 */5 * * * ?}")
    public void syncMeetings() {
        log.info("【MeetingTask】开始执行会议行同步与建会");
        try {
            meetingSyncManager.syncMeetingRecordsFromSheets();
            meetingSyncManager.createPendingWeComMeetings();
        } catch (Exception e) {
            log.error("【MeetingTask】会议同步失败", e);
        }
    }
}
