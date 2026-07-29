package com.cacch.integration.integration.sharedrive.support;

import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * 共享盘文件辅助（校验和、MIME）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class ShareDriveFileSupport {

    private static final Map<String, String> EXTENSION_CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("zip", "application/zip")
    );

    private ShareDriveFileSupport() {
    }

    /**
     * 计算 SHA-256
     *
     * @param content 文件字节
     * @return 十六进制小写；content 为空时返回 null
     */
    public static String sha256(byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 由 MessageDigest 输出十六进制 SHA-256
     *
     * @param digest 已完成更新的摘要实例
     * @return 十六进制小写；digest 为 null 时返回 null
     */
    public static String sha256Hex(MessageDigest digest) {
        if (digest == null) {
            return null;
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * 按扩展名推断 MIME
     *
     * @param fileName 文件名
     * @return MIME；未知时返回 {@code application/octet-stream}
     */
    public static String guessContentType(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "application/octet-stream";
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return EXTENSION_CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    /**
     * 判断扩展名是否允许
     *
     * @param fileName          文件名
     * @param allowedExtensions 允许扩展名（小写，不含点）
     * @return true 表示允许
     */
    public static boolean isAllowedExtension(String fileName, Iterable<String> allowedExtensions) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return false;
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        for (String allowed : allowedExtensions) {
            if (allowed != null && allowed.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }
}
