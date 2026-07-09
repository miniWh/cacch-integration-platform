package com.cacch.integration.integration.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 三方 HTTP 入参/响应 INFO 日志工具。
 *
 * <p>打印完整业务报文，并对 Token、secret、密码等敏感字段脱敏。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
public final class ThirdPartyHttpLogSupport {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final Pattern ACCESS_TOKEN_IN_URL = Pattern.compile(
            "(?i)([?&]access_token=)[^&]*");
    private static final Pattern CORPSECRET_IN_URL = Pattern.compile(
            "(?i)([?&]corpsecret=)[^&]*");
    private static final Pattern SECRET_IN_URL = Pattern.compile(
            "(?i)([?&](?:secret|secret_key|secretid|secret_id|key)=)[^&]*");
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(?i)(\"(?:access_token|token|corpsecret|secret|secret_id|secret_key|password|passwd|authorization)\"\\s*:\\s*\")([^\"]*)(\")");

    private ThirdPartyHttpLogSupport() {
    }

    /**
     * 打印三方请求入参（INFO）。
     *
     * @param bizTag  日志前缀业务标识，如 WeComMeeting
     * @param action  接口动作说明
     * @param url     请求 URL（会脱敏）
     * @param request 请求体或查询参数对象，可为 null
     */
    public static void logRequest(String bizTag, String action, String url, Object request) {
        log.info("【{}】{}请求, url={}, request={}",
                bizTag, action, maskUrl(url), toJson(request));
    }

    /**
     * 打印三方响应报文（INFO）。
     *
     * @param bizTag   日志前缀业务标识
     * @param action   接口动作说明
     * @param response 响应对象，可为 null
     */
    public static void logResponse(String bizTag, String action, Object response) {
        log.info("【{}】{}响应, response={}", bizTag, action, toJson(response));
    }

    /**
     * 将对象序列化为 JSON 字符串，并对敏感字段脱敏。
     */
    public static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence text) {
            return maskSensitiveJson(text.toString());
        }
        if (value instanceof byte[] bytes) {
            if (bytes.length == 0) {
                return "[]";
            }
            String asText = new String(bytes, StandardCharsets.UTF_8);
            if (looksLikeText(asText)) {
                return maskSensitiveJson(asText);
            }
            return "{\"byteLength\":" + bytes.length + "}";
        }
        try {
            return maskSensitiveJson(MAPPER.writeValueAsString(value));
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * URL 脱敏：隐藏 access_token / corpsecret / secret 等查询参数。
     */
    public static String maskUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String masked = ACCESS_TOKEN_IN_URL.matcher(url).replaceAll("$1****");
        masked = CORPSECRET_IN_URL.matcher(masked).replaceAll("$1****");
        return SECRET_IN_URL.matcher(masked).replaceAll("$1****");
    }

    /**
     * 构建便于日志输出的查询参数 Map（保序）。
     */
    public static Map<String, String> queryParams(String... keyValues) {
        Map<String, String> params = new LinkedHashMap<>();
        if (keyValues == null) {
            return params;
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            if (key != null) {
                params.put(key, value);
            }
        }
        return params;
    }

    private static String maskSensitiveJson(String json) {
        if (!StringUtils.hasText(json)) {
            return json;
        }
        return SENSITIVE_JSON_FIELD.matcher(json).replaceAll("$1****$3");
    }

    private static boolean looksLikeText(String text) {
        int sample = Math.min(text.length(), 64);
        for (int i = 0; i < sample; i++) {
            char c = text.charAt(i);
            if (c == 0) {
                return false;
            }
        }
        return true;
    }
}
