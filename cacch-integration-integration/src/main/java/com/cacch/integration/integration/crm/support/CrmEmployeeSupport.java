package com.cacch.integration.integration.crm.support;

import tools.jackson.databind.JsonNode;

/**
 * 勤策员工查询响应解析
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmEmployeeSupport {

    private CrmEmployeeSupport() {
    }

    /**
     * 从 queryEmployee 的 response_data 中解析首条员工节点
     *
     * <p>兼容单对象、数组、或含 {@code data}/{@code list}/{@code rows} 的包装结构。</p>
     *
     * @param responseData CRM {@code response_data}，可空
     * @return 员工 JSON 节点；无法解析时返回 null
     */
    public static JsonNode firstEmployeeNode(JsonNode responseData) {
        return unwrap(responseData);
    }

    /**
     * 读取员工字段（字符串）
     *
     * @param employee 员工节点
     * @param field    字段名
     * @return 非空字符串；否则 null
     */
    public static String text(JsonNode employee, String field) {
        if (employee == null || field == null) {
            return null;
        }
        JsonNode node = employee.get(field);
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String value = node.asString();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static JsonNode unwrap(JsonNode raw) {
        if (raw == null || raw.isNull() || raw.isMissingNode()) {
            return null;
        }
        if (raw.isArray()) {
            return raw.isEmpty() ? null : raw.get(0);
        }
        if (raw.isObject()) {
            if (raw.has("emp_code") || raw.has("id") || raw.has("emp_id")) {
                return raw;
            }
            for (String key : new String[]{"data", "list", "rows", "records", "employees"}) {
                if (raw.has(key)) {
                    JsonNode nested = unwrap(raw.get(key));
                    if (nested != null) {
                        return nested;
                    }
                }
            }
        }
        return null;
    }
}
