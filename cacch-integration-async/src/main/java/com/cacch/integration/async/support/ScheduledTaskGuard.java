package com.cacch.integration.async.support;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时任务进程内互斥：上一轮未结束则跳过本次，避免同任务叠跑。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
public final class ScheduledTaskGuard {

    private ScheduledTaskGuard() {
    }

    /**
     * 在 TraceId 上下文中互斥执行任务；若已有同任务在跑则 INFO 跳过。
     *
     * @param taskName 任务名称（用于日志）
     * @param running  任务专属运行标志
     * @param task     业务逻辑
     */
    public static void runExclusive(String taskName, AtomicBoolean running, Runnable task) {
        if (!running.compareAndSet(false, true)) {
            log.info("【MeetingTask】跳过{}, reason=上一轮仍在执行", taskName);
            return;
        }
        try {
            ScheduledTaskTraceSupport.runWithTraceId(task);
        } finally {
            running.set(false);
        }
    }
}
