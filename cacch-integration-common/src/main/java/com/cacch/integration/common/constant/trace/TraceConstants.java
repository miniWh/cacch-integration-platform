package com.cacch.integration.common.constant.trace;

/**
 * 链路追踪 TraceId 常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class TraceConstants {

    private TraceConstants() {
    }

    /**
     * SLF4J MDC 中的 TraceId 键名（与 logback-spring.xml 中 %X{traceId} 对应）
     */
    public static final String MDC_TRACE_ID = "traceId";

    /**
     * HTTP 请求/响应头，用于跨服务传递 TraceId
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
}
