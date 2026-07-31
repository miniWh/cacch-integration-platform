package com.cacch.integration.async.oa.task;

import com.cacch.integration.async.support.ScheduledTaskGuard;
import com.cacch.integration.common.config.oa.ShareDirProvisionProperties;
import com.cacch.integration.common.constant.oa.ShareDirProvisionConstants;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.manager.oa.api.IOaRegShareDirectoryProvisionManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 共享盘目录治理定时任务（REQ-OA-002）
 *
 * <p>定时读取 OA 资料列表（含「需要 / 不需要」），按 L3 路径分组后探测共享盘，
 * 自动创建缺失目录、安全删除空 L3。建议 cron 早于附件同步任务。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "oa.share-dir-provision", name = "enabled", havingValue = "true")
public class OaRegShareDirectoryProvisionTask {

    private static final String BIZ = "oa";
    private static final String TASK_NAME = "共享盘目录治理";

    private final IOaRegShareDirectoryProvisionManager provisionManager;
    private final IWeComWebhookManager weComWebhookManager;
    private final ShareDirProvisionProperties provisionProperties;
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 定时执行目录治理（全量游标分批）。
     *
     * <p>触发频率由 {@code oa.share-dir-provision.cron} 配置；上一轮未结束则跳过。</p>
     */
    @Scheduled(cron = "${oa.share-dir-provision.cron:0 0 * * * ?}")
    public void provisionShareDirectories() {
        ScheduledTaskGuard.runExclusive(TASK_NAME, running, () -> {
            log.info("【{}】开始执行{}, enabled={}, formBatchSize={}",
                    ShareDirProvisionConstants.LOG_BIZ, TASK_NAME,
                    provisionProperties.isEnabled(), provisionProperties.getFormBatchSize());
            try {
                provisionManager.provisionDirectories(null);
            } catch (Exception e) {
                log.info("【{}】{}异常终止, reason={}", ShareDirProvisionConstants.LOG_BIZ, TASK_NAME, e.getMessage());
                log.error("【{}】{}失败", ShareDirProvisionConstants.LOG_BIZ, TASK_NAME, e);
                weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                        .biz(BIZ)
                        .title("定时目录治理任务异常")
                        .subject(TASK_NAME)
                        .error(e)
                        .dedupType("task")
                        .dedupId(ShareDirProvisionConstants.LOG_BIZ)
                        .mention(true)
                        .build());
            }
        });
    }
}
