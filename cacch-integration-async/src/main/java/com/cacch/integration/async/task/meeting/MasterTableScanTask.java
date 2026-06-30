package com.cacch.integration.async.task.meeting;

import com.cacch.integration.manager.meeting.api.IMeetingSyncManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 总控表扫描定时任务 — 扫描已批准申请并创建员工会议管理智能表格
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterTableScanTask {

    private final IMeetingSyncManager meetingSyncManager;

    @Scheduled(cron = "${meeting.sync.master-cron:0 */10 * * * ?}")
    public void scanMasterTable() {
        log.info("【MeetingTask】开始执行总控表扫描");
        try {
            meetingSyncManager.scanMasterAndProvision();
        } catch (Exception e) {
            log.error("【MeetingTask】总控表扫描失败", e);
        }
    }
}
