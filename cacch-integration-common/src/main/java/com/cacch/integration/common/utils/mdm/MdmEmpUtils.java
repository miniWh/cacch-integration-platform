package com.cacch.integration.common.utils.mdm;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 人员主数据（MDM）人员子域特有工具方法
 *
 * <p>仅承载人员子域特有的解析逻辑；SHA-256 哈希、空值处理等跨子域公共方法
 * 见 {@link MdmUtils}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmEmpUtils {

    private MdmEmpUtils() {
    }

    /**
     * iHR 日期字段格式（yyyy-MM-dd）
     */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 解析 iHR 日期字符串（yyyy-MM-dd；兼容带时分秒的 yyyy-MM-dd HH:mm:ss）
     *
     * <p>解析失败按 null 处理并打 WARN（单字段脏数据不阻断整条落库）。</p>
     *
     * @param dateStr   日期字符串（可为 null 或空）
     * @param staffId   员工 ID（日志定位用）
     * @param fieldName 字段名（日志定位用）
     * @return 解析后的 LocalDate；入参为空或格式非法时返回 null
     */
    public static LocalDate parseDate(String dateStr, String staffId, String fieldName) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        String trimmed = dateStr.trim();
        try {
            return LocalDate.parse(trimmed.length() > 10 ? trimmed.substring(0, 10) : trimmed, DATE_FMT);
        } catch (Exception e) {
            logWarn(staffId, fieldName, trimmed);
            return null;
        }
    }

    /**
     * Long → String（null 安全；iHR departmentId 是 Long，ODS 与任职表存字符串形式）
     *
     * @param value Long 值（可为 null）
     * @return 字符串；入参为 null 时返回 null（与 ODS 字段 null 语义一致）
     */
    public static String longToStringSafe(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 日期解析失败 WARN 日志输出（工具类内集中实现）
     */
    private static void logWarn(String staffId, String fieldName, String value) {
        // SLF4J 静态初始化（工具类无 Spring 注入）
        org.slf4j.LoggerFactory.getLogger(MdmEmpUtils.class)
                .warn("【人员主数据】日期解析失败按空处理, ihrStaffId={}, field={}, value={}",
                        staffId, fieldName, value);
    }
}
