package com.cacch.integration.integration.crm.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * CRM 采集时间窗（Asia/Shanghai）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmCollectTimeWindowSupport {

    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    private CrmCollectTimeWindowSupport() {
    }

    /**
     * 默认当天时间窗：00:00:00（含）~ 次日 00:00:00（不含）
     *
     * <p>因查询使用 GT/LT：begin 取「当天 00:00:00 前 1ms」，end 取「次日 00:00:00」。</p>
     *
     * @return [beginEpochMilli, endEpochMilli]
     */
    public static String[] todayWindowEpochMilli() {
        LocalDate today = LocalDate.now(ZONE_SHANGHAI);
        long startInclusive = LocalDateTime.of(today, LocalTime.MIN)
                .atZone(ZONE_SHANGHAI).toInstant().toEpochMilli();
        long endExclusive = LocalDateTime.of(today.plusDays(1), LocalTime.MIN)
                .atZone(ZONE_SHANGHAI).toInstant().toEpochMilli();
        return new String[]{String.valueOf(startInclusive - 1L), String.valueOf(endExclusive)};
    }

    /**
     * 将日期时间字符串转为毫秒，并构造 GT 友好的 begin（减 1ms）
     *
     * @param beginDateTime yyyy-MM-dd HH:mm:ss 或毫秒串
     * @param endDateTime   yyyy-MM-dd HH:mm:ss 或毫秒串，可空
     * @return [beginEpochMilli, endEpochMilli]；end 可空
     */
    public static String[] toQueryWindow(String beginDateTime, String endDateTime) {
        String beginMilli = CrmTimeSupport.toEpochMilliString(beginDateTime, "beginTime");
        String beginForGt = String.valueOf(Long.parseLong(beginMilli) - 1L);
        String endMilli = null;
        if (endDateTime != null && !endDateTime.isBlank()) {
            endMilli = CrmTimeSupport.toEpochMilliString(endDateTime, "endTime");
        }
        return new String[]{beginForGt, endMilli};
    }
}
