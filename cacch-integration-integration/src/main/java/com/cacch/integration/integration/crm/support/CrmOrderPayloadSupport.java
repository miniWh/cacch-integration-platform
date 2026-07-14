package com.cacch.integration.integration.crm.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CRM 订单 / 明细响应解析
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmOrderPayloadSupport {

    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private CrmOrderPayloadSupport() {
    }

    /**
     * 从 orderQuery / orderDetailQuery 的 response_data 解析列表节点
     *
     * @param responseData response_data，可空
     * @return 记录列表；无数据时返回空列表
     */
    public static List<JsonNode> listRecords(JsonNode responseData) {
        JsonNode listNode = unwrapList(responseData);
        if (listNode == null || !listNode.isArray()) {
            return Collections.emptyList();
        }
        List<JsonNode> records = new ArrayList<>(listNode.size());
        listNode.forEach(records::add);
        return records;
    }

    /**
     * 读取字符串字段
     *
     * @param node  节点
     * @param field 字段名
     * @return 非空字符串；否则 null
     */
    public static String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode child = node.get(field);
        return asText(child);
    }

    /**
     * 读取嵌套对象的字符串字段，如 {@code customer.id}
     *
     * @param node        根节点
     * @param objectField 对象字段名
     * @param nestedField 嵌套字段名
     * @return 非空字符串；否则 null
     */
    public static String nestedText(JsonNode node, String objectField, String nestedField) {
        if (node == null) {
            return null;
        }
        JsonNode obj = node.get(objectField);
        if (obj == null || obj.isNull() || !obj.isObject()) {
            return null;
        }
        return asText(obj.get(nestedField));
    }

    /**
     * 将 CRM 时间字段解析为上海时区 LocalDateTime
     *
     * <p>兼容：毫秒/秒时间戳字符串或数字；{@code yyyy-MM-dd HH:mm:ss}。</p>
     *
     * @param timeNode 时间节点，可空
     * @return 本地时间；无法解析时返回 null
     */
    public static LocalDateTime parseCrmDateTime(JsonNode timeNode) {
        String text = asText(timeNode);
        if (text == null) {
            return null;
        }
        if (text.chars().allMatch(Character::isDigit)) {
            long value = Long.parseLong(text);
            long millis = text.length() <= 10 ? value * 1000L : value;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZONE_SHANGHAI);
        }
        try {
            return LocalDateTime.parse(text, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * JsonNode 转为可入库 Object
     *
     * @param node JSON 节点
     * @return Object；空节点返回空 Map 占位
     */
    public static Object toJsonObject(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return Collections.emptyMap();
        }
        return MAPPER.convertValue(node, Object.class);
    }

    /**
     * 将入库的 raw_payload（Map/JSON 字符串等）转为 JsonNode
     *
     * @param raw 原始对象，可空
     * @return JsonNode；无法转换时返回 missing node
     */
    public static JsonNode asJsonNode(Object raw) {
        if (raw == null) {
            return MAPPER.missingNode();
        }
        if (raw instanceof JsonNode node) {
            return node;
        }
        if (raw instanceof String text) {
            if (text.isBlank()) {
                return MAPPER.missingNode();
            }
            try {
                return MAPPER.readTree(text);
            } catch (Exception e) {
                return MAPPER.missingNode();
            }
        }
        try {
            return MAPPER.valueToTree(raw);
        } catch (Exception e) {
            return MAPPER.missingNode();
        }
    }

    private static JsonNode unwrapList(JsonNode raw) {
        if (raw == null || raw.isNull() || raw.isMissingNode()) {
            return null;
        }
        if (raw.isArray()) {
            return raw;
        }
        if (raw.isObject()) {
            for (String key : new String[]{"rows", "list", "data", "records", "result"}) {
                if (raw.has(key)) {
                    JsonNode nested = unwrapList(raw.get(key));
                    if (nested != null) {
                        return nested;
                    }
                }
            }
        }
        return null;
    }

    private static String asText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isObject() || node.isArray()) {
            return null;
        }
        String value = node.asString();
        return value == null || value.isBlank() ? null : value.trim();
    }
}
