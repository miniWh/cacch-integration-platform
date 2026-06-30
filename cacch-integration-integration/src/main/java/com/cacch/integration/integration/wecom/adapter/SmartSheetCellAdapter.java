package com.cacch.integration.integration.wecom.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能表格单元格值构建与解析适配器
 */
public final class SmartSheetCellAdapter {

    private SmartSheetCellAdapter() {
    }

    /**
     * 构建文本类型单元格值
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
     * 从单元格原始值中提取文本（兼容 List 结构和纯字符串）
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
     * 按列映射 key 从 record values 中取文本
     */
    public static String getMappedText(Map<String, Object> values, Map<String, String> columnMapping, String logicalKey) {
        if (values == null || columnMapping == null) {
            return "";
        }
        String fieldId = columnMapping.get(logicalKey);
        if (fieldId == null) {
            return "";
        }
        return extractText(values.get(fieldId));
    }
}
