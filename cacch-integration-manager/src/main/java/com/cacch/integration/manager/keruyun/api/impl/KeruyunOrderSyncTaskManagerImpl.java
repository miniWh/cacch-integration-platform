package com.cacch.integration.manager.keruyun.api.impl;

import com.cacch.integration.common.constant.keruyun.KeruyunOrderSyncStatus;
import com.cacch.integration.common.dto.keruyun.KeruyunOrderSyncBatchResult;
import com.cacch.integration.entity.keruyun.KeruyunOrderSyncTaskDO;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderManager;
import com.cacch.integration.manager.keruyun.api.IKeruyunOrderSyncTaskManager;
import com.cacch.integration.service.keruyun.api.IKeruyunOrderSyncTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 客如云订单同步任务编排实现
 *
 * <p>提供批量执行入口（两步编排）与单任务异步执行入口。
 * 单天失败不阻断后续天，任务间通过数据库唯一键与 CAS 保证幂等。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeruyunOrderSyncTaskManagerImpl implements IKeruyunOrderSyncTaskManager {

    private static final String BIZ = "客如云订单同步任务编排";

    private final IKeruyunOrderSyncTaskService taskService;
    private final IKeruyunOrderManager orderManager;

    @Override
    public KeruyunOrderSyncBatchResult executeBatch(LocalDate anchorDate, String dateType) {
        // 1. 确定日期范围
        LocalDate targetDate = anchorDate != null ? anchorDate : LocalDate.now().minusDays(1);
        if (anchorDate == null) {
            log.info("【{}】基准日期未指定, 默认取昨天={}", BIZ, targetDate);
        }

        // 2. 第一步：检查并新建缺失的 PENDING 任务
        int created = taskService.createMissingTasks(targetDate, targetDate);

        // 3. 第二步：取所有 PENDING 任务（按日期升序）
        List<KeruyunOrderSyncTaskDO> pending = taskService.findAllPending();

        // 4. 按 dateType 过滤（不传时默认 OPEN_TIME，避免跑全部 4 种）
        String effectiveDateType = StringUtils.hasText(dateType) ? dateType : "OPEN_TIME";
        pending = pending.stream()
                .filter(t -> effectiveDateType.equals(t.getDateType()))
                .toList();

        // 5. 逐条异步触发（单天失败不阻断后续天）
        List<Long> taskIds = new ArrayList<>();
        for (KeruyunOrderSyncTaskDO task : pending) {
            Long id = task.getId();
            taskIds.add(id);
            try {
                CompletableFuture.runAsync(() -> executeSingleTask(id));
            } catch (Exception e) {
                log.error("【{}】任务触发异步执行失败, id={}, reason={}", BIZ, id, e.getMessage(), e);
                taskService.markComplete(id, KeruyunOrderSyncStatus.FAILED.name(),
                        "异步执行触发失败: " + e.getMessage());
            }
        }

        String message = String.format("基准日期=%s, dateType=%s, 本次新建任务=%d, 待执行任务=%d",
                targetDate, effectiveDateType, created, pending.size());
        log.info("【{}】批次触发完成, {}", BIZ, message);
        return new KeruyunOrderSyncBatchResult(created, pending.size(), taskIds, message);
    }

    @Override
    @Async("keruyunOrderSyncExecutor")
    public void executeSingleTask(Long taskId) {
        log.info("【{}】开始执行任务, id={}", BIZ, taskId);
        try {
            orderManager.syncByTaskId(taskId);
            log.info("【{}】任务执行完成, id={}", BIZ, taskId);
        } catch (Exception e) {
            log.error("【{}】任务执行异常, id={}, reason={}", BIZ, taskId, e.getMessage(), e);
            taskService.markComplete(taskId, KeruyunOrderSyncStatus.FAILED.name(), e.getMessage());
        }
    }
}
