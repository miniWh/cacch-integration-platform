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
     * 从成员列原始值中解析 userId 列表
     */
    public static List<String> extractUserIds(Object cellValue) {
        if (!(cellValue instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<String> userIds = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object userId = map.get("user_id");
                if (userId != null && !userId.toString().isBlank()) {
                    userIds.add(userId.toString());
                }
            }
        }
        return userIds;
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
     * 判断映射值是否为企微 fieldId（历史数据兼容，fieldId 为短字母数字串）
     */
    public static boolean looksLikeFieldId(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.matches("^[a-zA-Z][a-zA-Z0-9]{3,15}$");
    }
}
