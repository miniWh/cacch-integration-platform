package com.cacch.integration.integration.oa.support;

import com.cacch.integration.integration.oa.client.dto.OaOrgMember;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 致远 OA 响应解析辅助
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaResponseSupport {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private OaResponseSupport() {
    }

    /**
     * 从按编码取人员响应中解析首个人员
     *
     * <p>兼容：单对象、数组、或含 {@code data}/{@code pageData} 的分页结构。</p>
     *
     * @param raw 原始 JSON 节点，可空
     * @return 人员对象；无法解析时返回 null
     */
    public static OaOrgMember firstOrgMember(JsonNode raw) {
        JsonNode memberNode = unwrapMemberNode(raw);
        if (memberNode == null || memberNode.isNull() || memberNode.isMissingNode()) {
            return null;
        }
        return MAPPER.convertValue(memberNode, OaOrgMember.class);
    }

    /**
     * 从附件上传响应中解析 fileUrl（文件 ID）
     *
     * <p>兼容：{@code atts[0].fileUrl}、{@code atts[0].fileurl}、顶层 {@code fileUrl} 等。</p>
     *
     * @param raw 原始响应，可空
     * @return 文件 ID；无法解析时返回 null
     */
    public static String extractFileUrl(JsonNode raw) {
        if (raw == null || raw.isNull() || raw.isMissingNode()) {
            return null;
        }
        if (raw.isArray() && !raw.isEmpty()) {
            return extractFileUrlFromAttachmentNode(raw.get(0));
        }
        if (raw.isObject()) {
            String direct = firstNonBlankText(raw, "fileUrl", "fileurl", "file_url");
            if (direct != null) {
                return direct;
            }
            JsonNode atts = raw.get("atts");
            if (atts != null && atts.isArray() && !atts.isEmpty()) {
                return extractFileUrlFromAttachmentNode(atts.get(0));
            }
            for (String key : new String[]{"data", "result", "content"}) {
                if (raw.has(key)) {
                    String nested = extractFileUrl(raw.get(key));
                    if (nested != null) {
                        return nested;
                    }
                }
            }
        }
        return null;
    }

    private static String extractFileUrlFromAttachmentNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return firstNonBlankText(node, "fileUrl", "fileurl", "file_url", "id");
    }

    /**
     * 从发起流程响应中解析流程实例 ID
     *
     * <p>兼容：纯字符串、常见字段 {@code processId}/{@code id}/{@code summaryId} 及嵌套 {@code data}。</p>
     *
     * @param raw 原始响应，可空
     * @return 流程实例 ID；无法解析时返回 null
     */
    public static String extractProcessId(JsonNode raw) {
        if (raw == null || raw.isNull() || raw.isMissingNode()) {
            return null;
        }
        if (raw.isValueNode()) {
            String text = raw.asString();
            return text == null || text.isBlank() ? null : text.trim();
        }
        String direct = firstNonBlankText(raw,
                "processId", "process_id", "id", "summaryId", "affairId",
                "app_bussiness_data", "appBussinessData");
        if (direct != null) {
            return direct;
        }
        for (String key : new String[]{"data", "result", "content"}) {
            if (raw.has(key)) {
                String nested = extractProcessId(raw.get(key));
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    /**
     * 判断 CAP4 batch-update 是否全部成功
     *
     * @param raw batch-update 原始响应，可空
     * @return {@code code=0} 且 {@code failedCount=0} 时返回 true
     */
    public static boolean isCap4BatchUpdateSuccess(JsonNode raw) {
        if (raw == null || raw.isNull() || raw.isMissingNode() || !raw.isObject()) {
            return false;
        }
        JsonNode codeNode = raw.get("code");
        if (codeNode == null || codeNode.isNull() || codeNode.isMissingNode()) {
            return false;
        }
        int code = codeNode.isNumber() ? codeNode.intValue() : parseIntOrMinusOne(codeNode.asString());
        if (code != 0) {
            return false;
        }
        JsonNode data = raw.get("data");
        if (data == null || data.isNull() || data.isMissingNode() || !data.isObject()) {
            return true;
        }
        JsonNode failedCount = data.get("failedCount");
        if (failedCount == null || failedCount.isNull() || failedCount.isMissingNode()) {
            return true;
        }
        return failedCount.isNumber() ? failedCount.intValue() == 0 : parseIntOrMinusOne(failedCount.asString()) == 0;
    }

    /**
     * 提取 CAP4 batch-update 失败原因摘要
     *
     * @param raw batch-update 原始响应，可空
     * @return 失败原因；无法解析时返回 null
     */
    public static String extractCap4BatchUpdateFailureMessage(JsonNode raw) {
        if (raw == null || raw.isNull() || raw.isMissingNode() || !raw.isObject()) {
            return null;
        }
        String message = firstNonBlankText(raw, "message");
        JsonNode data = raw.get("data");
        if (data != null && data.isObject()) {
            JsonNode failedData = data.get("failedData");
            if (failedData != null && failedData.isObject() && !failedData.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                failedData.properties().forEach(entry ->
                        sb.append(entry.getKey()).append('=').append(entry.getValue().asString()).append(';'));
                if (!sb.isEmpty()) {
                    return message == null ? sb.toString() : message + " | " + sb;
                }
            }
        }
        return message;
    }

    private static int parseIntOrMinusOne(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String firstNonBlankText(JsonNode node, String... fields) {
        if (node == null || !node.isObject() || fields == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode child = node.get(field);
            if (child == null || child.isNull() || child.isMissingNode() || child.isObject() || child.isArray()) {
                continue;
            }
            String text = child.asString();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    private static JsonNode unwrapMemberNode(JsonNode raw) {
        if (raw == null || raw.isNull() || raw.isMissingNode()) {
            return null;
        }
        if (raw.isArray()) {
            return raw.isEmpty() ? null : raw.get(0);
        }
        if (raw.isObject()) {
            if (raw.has("id") || raw.has("loginName") || raw.has("code")) {
                return raw;
            }
            for (String key : new String[]{"data", "pageData", "list", "orgMembers", "members"}) {
                if (raw.has(key)) {
                    JsonNode nested = unwrapMemberNode(raw.get(key));
                    if (nested != null) {
                        return nested;
                    }
                }
            }
        }
        return null;
    }
}
