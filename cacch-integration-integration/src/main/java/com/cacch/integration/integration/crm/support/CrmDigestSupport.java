package com.cacch.integration.integration.crm.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 勤策 CRM OpenAPI 签名与消息 ID 工具
 *
 * <p>官方规范：{@code digest = MD5(jsonBody + "|" + appKey + "|" + timestamp)}，
 * 签名用的消息体必须与 HTTP 请求体字节级一致。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class CrmDigestSupport {

    private CrmDigestSupport() {
    }

    /**
     * 计算 digest 签名
     *
     * @param jsonBody  与请求体完全一致的 JSON 字符串
     * @param appKey    企业 appkey
     * @param timestamp 毫秒时间戳字符串
     * @return 32 位小写 MD5 hex
     */
    public static String digest(String jsonBody, String appKey, String timestamp) {
        String joint = jsonBody + "|" + appKey + "|" + timestamp;
        return md5Hex(joint);
    }

    /**
     * 生成消息 ID（UUID 去横线）
     *
     * @return 32 位消息 ID
     */
    public static String newMsgId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 当前毫秒时间戳字符串
     *
     * @return timestamp
     */
    public static String currentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    private static String md5Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}
