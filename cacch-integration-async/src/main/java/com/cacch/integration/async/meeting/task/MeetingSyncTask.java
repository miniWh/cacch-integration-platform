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
 * 会议行同步与建会定时任务
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingSyncTask {

    private static final String BIZ = "meeting";
    private static final String TASK_NAME = "会议行同步与建会";

    private final IMeetingSyncManager meetingSyncManager;
    private final IWeComWebhookManager weComWebhookManager;

    /**
     * 定时同步会议行并为待处理会议创建企微预约会议
     */
    @Scheduled(cron = "${meeting.sync.meeting-cron:0 */5 * * * ?}")
    public void syncMeetings() {
        ScheduledTaskTraceSupport.runWithTraceId(() -> {
            log.info("【MeetingTask】开始执行{}", TASK_NAME);
            try {
                meetingSyncManager.syncMeetingRecordsFromSheets();
                meetingSyncManager.createPendingWeComMeetings();
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
