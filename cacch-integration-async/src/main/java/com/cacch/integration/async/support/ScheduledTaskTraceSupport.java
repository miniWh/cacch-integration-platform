package com.cacch.integration.async.support;

import com.cacch.integration.common.constant.trace.TraceConstants;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 定时任务 TraceId 工具
 * @author hongfu_zhou@cacch.com
 */
public final class ScheduledTaskTraceSupport {

    private ScheduledTaskTraceSupport() {
    }

    public static void runWithTraceId(Runnable task) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(TraceConstants.MDC_TRACE_ID, traceId);
        try {
            task.run();
        } finally {
            MDC.remove(TraceConstants.MDC_TRACE_ID);
        }
    }
}
