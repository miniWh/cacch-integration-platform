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

    /**
     * 定时扫描总控表并创建员工会议管理智能表格
     */
    @Scheduled(cron = "${meeting.sync.master-cron:0 */3 * * * ?}")
    public void scanMasterTable() {
        ScheduledTaskTraceSupport.runWithTraceId(() -> {
            log.info("【MeetingTask】开始执行{}", TASK_NAME);
            try {
                meetingSyncManager.scanMasterAndProvision();
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
