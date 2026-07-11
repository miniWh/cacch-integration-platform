package com.cacch.integration.integration.crm.support;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 勤策时间参数转换：对外可传日期时间字符串，调用 CRM 时转为毫秒时间戳字符串
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmTimeSupport {

    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CrmTimeSupport() {
    }

    /**
     * 将时间入参转为毫秒时间戳字符串（CRM modify_time 过滤要求）
     *
     * <p>支持：</p>
     * <ul>
     *   <li>纯数字：视为已是毫秒时间戳（或 10 位秒级，自动乘 1000）</li>
     *   <li>{@code yyyy-MM-dd HH:mm:ss}：按 Asia/Shanghai 解析</li>
     * </ul>
     *
     * @param timeInput 时间入参，不可为空
     * @param fieldName 字段名（用于异常提示）
     * @return 毫秒时间戳字符串，如 {@code 1719763200000}
     */
    public static String toEpochMilliString(String timeInput, String fieldName) {
        if (timeInput == null || timeInput.isBlank()) {
            throw new BizException(ResultCode.PARAM_MISSING, fieldName + " 不能为空");
        }
        String trimmed = timeInput.trim();
        if (trimmed.chars().allMatch(Character::isDigit)) {
            if (trimmed.length() == 10) {
                return String.valueOf(Long.parseLong(trimmed) * 1000L);
            }
            return trimmed;
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(trimmed, DATE_TIME);
            long millis = ldt.atZone(ZONE_SHANGHAI).toInstant().toEpochMilli();
            return String.valueOf(millis);
        } catch (DateTimeParseException e) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    fieldName + " 格式无效，请传毫秒时间戳或 yyyy-MM-dd HH:mm:ss", e);
        }
    }
}
