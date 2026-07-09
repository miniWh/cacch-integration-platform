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
 * 会议行同步、建会与企微详情反向同步定时任务
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingSyncTask {

    private static final String BIZ = "meeting";
    private static final String TASK_NAME = "会议行同步与建会";
    private static final String REVERSE_SYNC_TASK_NAME = "企微会议详情反向同步";

    private final IMeetingSyncManager meetingSyncManager;
    private final IWeComWebhookManager weComWebhookManager;

    /**
     * 定时扫描会议管理子表：同步行数据、按规则自动创建企微会议，并反向同步已创建会议详情
     */
    @Scheduled(cron = "${meeting.sync.meeting-cron:0 */3 * * * ?}")
    public void syncMeetings() {
        ScheduledTaskTraceSupport.runWithTraceId(() -> {
            log.info("【MeetingTask】开始执行{}", TASK_NAME);
            try {
                meetingSyncManager.syncMeetingRecordsFromSheets();
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
            log.info("【MeetingTask】开始执行{}", REVERSE_SYNC_TASK_NAME);
            try {
                meetingSyncManager.syncScheduledMeetingsFromWeCom();
            } catch (Exception e) {
                log.info("【MeetingTask】{}异常终止, reason={}", REVERSE_SYNC_TASK_NAME, e.getMessage());
                log.error("【MeetingTask】{}失败", REVERSE_SYNC_TASK_NAME, e);
                weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                        .biz(BIZ)
                        .title("定时同步任务异常")
                        .subject(REVERSE_SYNC_TASK_NAME)
                        .error(e)
                        .dedupType("task")
                        .dedupId(REVERSE_SYNC_TASK_NAME)
                        .mention(true)
                        .build());
            }
        });
    }
}
