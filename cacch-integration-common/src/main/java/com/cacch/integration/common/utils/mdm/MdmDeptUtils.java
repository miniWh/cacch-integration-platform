package com.cacch.integration.common.utils.mdm;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 部门主数据（MDM）域通用工具方法 —— 跨模块复用的算法类逻辑
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmDeptUtils {

    private MdmDeptUtils() {
    }

    /**
     * SHA-256 哈希（小写 hex）
     *
     * <p>用于 ods 行哈希（rowHash）、mdm_dept_event.event_id（事件幂等键）。</p>
     *
     * @param value 原文（可为 null，按空串处理）
     * @return 64 位小写 hex 哈希
     */
    public static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /**
     * null 或空串 → null（防止数据库存储 '' 污染）
     *
     * @param value 原值（可为 null 或空串）
     * @return null —— 空串或纯空白视为 null；否则返回原值
     */
    public static String nullIfBlank(String value) {
        return StringUtils.isBlank(value) ? null : value;
    }

    /**
     * null 转空串（rowHash 字段拼接用）
     *
     * @param value 原值（可为 null）
     * @return 非空字符串
     */
    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
