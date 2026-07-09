package com.cacch.integration.integration.wecom.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企微智能表格单元格值构建与解析适配器
 *
 * @author hongfu_zhou@cacch.com
 */
public final class WeComSmartSheetCellAdapter {

    private WeComSmartSheetCellAdapter() {
    }

    /**
     * 构建文本类型单元格值
     *
     * @param text 单元格文本，null 时按空字符串处理
     * @return 企微智能表格要求的单元格值结构
     */
    public static List<Map<String, String>> textCell(String text) {
        List<Map<String, String>> cells = new ArrayList<>(1);
        Map<String, String> cell = new HashMap<>(2);
        cell.put("type", "text");
        cell.put("text", text != null ? text : "");
        cells.add(cell);
        return cells;
    }

    /**
     * 构建超链接类型单元格值
     *
     * @param text 链接展示文本，null 时按空字符串处理
     * @param link 链接地址，null 时按空字符串处理
     * @return 企微智能表格要求的 URL 单元格值结构
     */
    public static List<Map<String, String>> urlCell(String text, String link) {
        List<Map<String, String>> cells = new ArrayList<>(1);
        Map<String, String> cell = new HashMap<>(3);
        cell.put("type", "url");
        cell.put("text", text != null ? text : "");
        cell.put("link", link != null ? link : "");
        cells.add(cell);
        return cells;
    }

    /**
     * 从单元格原始值中提取文本（兼容 List 结构和纯字符串）
     *
     * @param cellValue 企微返回的单元格原始值
     * @return 提取后的文本，无法解析时返回空字符串
     */
    public static String extractText(Object cellValue) {
        switch (cellValue) {
            case null -> {
                return "";
            }
            case String str -> {
                return str;
            }
            case List<?> list when !list.isEmpty() -> {
                Object first = list.getFirst();
                if (first instanceof Map<?, ?> map) {
                    Object type = map.get("type");
                    if ("url".equals(type)) {
                        Object link = map.get("link");
                        if (link != null && !link.toString().isBlank()) {
                            return link.toString();
                        }
                    }
                    Object text = map.get("text");
                    return text != null ? text.toString() : "";
                }
                return first.toString();
            }
            default -> {
            }
        }
        return cellValue.toString();
    }

    /**
     * 构建多成员类型单元格值
     */
    public static List<Map<String, String>> userCells(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> cells = new ArrayList<>(userIds.size());
        for (String userId : userIds) {
            if (userId != null && !userId.isBlank()) {
                Map<String, String> cell = new HashMap<>(2);
                cell.put("user_id", userId);
                cells.add(cell);
            }
        }
        return cells;
    }

    /**
     * 构建成员类型单元格值（单选人员）
     *
     * @param userId 企微成员 userId
     */
    public static List<Map<String, String>> userCell(String userId) {
        List<Map<String, String>> cells = new ArrayList<>(1);
        Map<String, String> cell = new HashMap<>(2);
        cell.put("user_id", userId != null ? userId : "");
        cells.add(cell);
        return cells;
    }

    /**
     * 按列映射 key 从 record values 中取文本
     *
     * @param values        行字段值 Map（fieldTitle → 单元格值）
     * @param columnMapping 逻辑列名 → 企微列标题 映射
     * @param logicalKey    逻辑列名（如 meeting_title）
     * @return 映射列的文本值，缺失时返回空字符串
     */
    public static String getMappedText(Map<String, Object> values, Map<String, String> columnMapping,
                                       String logicalKey) {
        if (values == null || columnMapping == null) {
            return "";
        }
        String fieldTitle = columnMapping.get(logicalKey);
        if (fieldTitle == null) {
            return "";
        }
        return extractText(values.get(fieldTitle));
    }

    /**
     * 从单元格原始值中提取数字（兼容 double / int / 文本）
     */
    public static Integer extractNumber(Object cellValue) {
        if (cellValue == null) {
            return null;
        }
        if (cellValue instanceof Number number) {
            return number.intValue();
        }
        String text = extractText(cellValue);
        if (text.isBlank()) {
            return null;
        }
        try {
            if (text.contains(".")) {
                return (int) Double.parseDouble(text.trim());
            }
            return Integer.parseInt(text.trim().replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从单选/多选列原始值中提取选项文本
     */
    public static String extractSelectText(Object cellValue) {
        if (!(cellValue instanceof List<?> list) || list.isEmpty()) {
            return extractText(cellValue);
        }
        Object first = list.getFirst();
        if (first instanceof Map<?, ?> map) {
            Object text = map.get("text");
            if (text != null && !text.toString().isBlank()) {
                return text.toString();
            }
        }
        return extractText(cellValue);
    }

    /**
     * 构建日期时间类型单元格值（企微要求以毫秒为单位的 Unix 时间戳字符串）
     *
     * @param dateTime 本地日期时间，null 时返回 null
     * @return 毫秒时间戳字符串
     */
    public static String dateTimeValue(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        java.time.LocalDateTime truncated = dateTime.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        long epochMilli = truncated.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return String.valueOf(epochMilli);
    }

    /**
     * 从日期时间列原始值解析为 LocalDateTime（企微返回毫秒时间戳字符串或数字）
     */
    public static java.time.LocalDateTime extractDateTime(Object cellValue) {
        if (cellValue == null) {
            return null;
        }
        if (cellValue instanceof Number number) {
            long epoch = number.longValue();
            if (epoch > 1_000_000_000_000L) {
                epoch /= 1000;
            }
            return java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(epoch), java.time.ZoneId.systemDefault());
        }
        String text = cellValue instanceof String str ? str.trim() : extractText(cellValue);
        if (text.isBlank()) {
            return null;
        }
        String normalized = text.replace('T', ' ');
        try {
            if (normalized.matches("\\d+")) {
                long epoch = Long.parseLong(normalized);
                if (epoch > 1_000_000_000_000L) {
                    epoch /= 1000;
                }
                return java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(epoch), java.time.ZoneId.systemDefault());
            }
            if (normalized.length() >= 16) {
                String dateTimePart = normalized.length() >= 19 ? normalized.substring(0, 19) : normalized.substring(0, 16);
                java.time.format.DateTimeFormatter formatter = normalized.length() >= 19
                        ? java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        : java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                return java.time.LocalDateTime.parse(dateTimePart, formatter);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    /**
     * 按列映射 key 从 record values 中取数字
     */
    public static Integer getMappedNumber(Map<String, Object> values, Map<String, String> columnMapping,
                                          String logicalKey) {
        if (values == null || columnMapping == null) {
            return null;
        }
        String fieldTitle = columnMapping.get(logicalKey);
        if (fieldTitle == null) {
            return null;
        }
        return extractNumber(values.get(fieldTitle));
    }

    /**
     * 按列映射 key 从 record values 中取单选文本
     */
    public static String getMappedSelectText(Map<String, Object> values, Map<String, String> columnMapping,
                                             String logicalKey) {
        if (values == null || columnMapping == null) {
            return "";
        }
        String fieldTitle = columnMapping.get(logicalKey);
        if (fieldTitle == null) {
            return "";
        }
        return extractSelectText(values.get(fieldTitle));
    }

    /**
     * 按列映射 key 从 record values 中取日期时间
     */
    public static java.time.LocalDateTime getMappedDateTime(Map<String, Object> values,
                                                            Map<String, String> columnMapping,
                                                            String logicalKey) {
        if (values == null || columnMapping == null) {
            return null;
        }
        String fieldTitle = columnMapping.get(logicalKey);
        if (fieldTitle == null) {
            return null;
        }
        return extractDateTime(values.get(fieldTitle));
    }

    /**
     * 从成员列原始值中解析 userId 列表（保序）
     *
     * <p>兼容企微返回的 {@code user_id} / {@code userid} / {@code id} 字段名。</p>
     */
    public static List<String> extractUserIds(Object cellValue) {
        if (!(cellValue instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<String> userIds = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object userId = map.get("user_id");
                if (userId == null) {
                    userId = map.get("userid");
                }
                if (userId == null) {
                    userId = map.get("id");
                }
                if (userId != null && !userId.toString().isBlank()) {
                    userIds.add(userId.toString().trim());
                }
            } else if (item instanceof String text && !text.isBlank()) {
                userIds.add(text.trim());
            }
        }
        return userIds;
    }

    /**
     * 从多选列原始值中提取选项文本列表
     */
    public static List<String> extractSelectTexts(Object cellValue) {
        if (!(cellValue instanceof List<?> list) || list.isEmpty()) {
            String single = extractSelectText(cellValue);
            return single.isBlank() ? List.of() : List.of(single);
        }
        List<String> texts = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object text = map.get("text");
                if (text != null && !text.toString().isBlank()) {
                    texts.add(text.toString());
                }
            }
        }
        return texts;
    }

    /**
     * 按列映射 key 从 record values 中取多选文本列表
     */
    public static List<String> getMappedSelectTexts(Map<String, Object> values, Map<String, String> columnMapping,
                                                    String logicalKey) {
        if (values == null || columnMapping == null) {
            return List.of();
        }
        String fieldTitle = columnMapping.get(logicalKey);
        if (fieldTitle == null) {
            return List.of();
        }
        return extractSelectTexts(values.get(fieldTitle));
    }

    /**
     * 按列映射 key 从 record values 中取成员 userId 列表
     */
    public static List<String> getMappedUserIds(Map<String, Object> values, Map<String, String> columnMapping,
                                                String logicalKey) {
        if (values == null || columnMapping == null) {
            return List.of();
        }
        String fieldTitle = columnMapping.get(logicalKey);
        if (fieldTitle == null) {
            return List.of();
        }
        return extractUserIds(values.get(fieldTitle));
    }

    /**
     * 从成员列原始值中取首位成员的展示名称（企微返回 name 字段时可用）
     */
    public static String extractFirstUserDisplayName(Object cellValue) {
        if (!(cellValue instanceof List<?> list) || list.isEmpty()) {
            return "";
        }
        Object item = list.get(0);
        if (item instanceof Map<?, ?> map) {
            Object name = map.get("name");
            if (name != null && !name.toString().isBlank()) {
                return name.toString();
            }
        }
        return "";
    }

    /**
     * 按列映射 key 从 record values 中取首位成员的展示名称
     */
    public static String getMappedFirstUserDisplayName(Map<String, Object> values, Map<String, String> columnMapping,
                                                       String logicalKey) {
        if (values == null || columnMapping == null) {
            return "";
        }
        String fieldTitle = columnMapping.get(logicalKey);
        if (fieldTitle == null) {
            return "";
        }
        return extractFirstUserDisplayName(values.get(fieldTitle));
    }

    /**
     * 判断映射值是否为企微 fieldId（历史数据兼容，fieldId 为短字母数字串）。
     * 映射已改为列标题时返回 false 属正常现象，不代表无法读取表格数据。
     */
    public static boolean looksLikeFieldId(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.matches("^[a-zA-Z][a-zA-Z0-9]{3,15}$");
    }
}
