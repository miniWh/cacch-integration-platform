package com.cacch.integration.integration.sharedrive.support;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 共享盘 L2 目录解析：{@code IPDP名称（项目编号）}，括号中英文均可
 *
 * @author hongfu_zhou@cacch.com
 */
public final class ShareDriveIpdpDirectorySupport {

    private static final Pattern L2_DIRECTORY_PATTERN = Pattern.compile("^(.*)[（(]([^）)]+)[）)]$");

    private ShareDriveIpdpDirectorySupport() {
    }

    /**
     * L2 目录解析结果
     *
     * @param directoryName 磁盘 L2 目录原名
     * @param ipdpName      IPDP 名称（括号外，对应 OA field0160）
     * @param ipdpProjectNo 项目编号（括号内，对应 OA field0164）
     */
    public record ParsedIpdpDirectory(String directoryName, String ipdpName, String ipdpProjectNo) {
    }

    /**
     * 从 L2 目录名解析 IPDP 名称与项目编号
     *
     * @param directoryName 共享盘 L2 目录名，如 {@code 10%环丙氟虫胺可分散液剂（IPDP-202508-089）}
     * @return 解析结果；格式不符合时返回 null
     */
    public static ParsedIpdpDirectory parse(String directoryName) {
        if (!StringUtils.hasText(directoryName)) {
            return null;
        }
        String trimmed = directoryName.trim();
        Matcher matcher = L2_DIRECTORY_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return null;
        }
        String ipdpName = matcher.group(1).trim();
        String ipdpProjectNo = matcher.group(2).trim();
        if (!StringUtils.hasText(ipdpName) || !StringUtils.hasText(ipdpProjectNo)) {
            return null;
        }
        return new ParsedIpdpDirectory(trimmed, ipdpName, ipdpProjectNo);
    }

    /**
     * 按 OA 字段拼接 L2 目录名（默认中文括号）
     *
     * @param ipdpName      IPDP 名称 field0160
     * @param ipdpProjectNo 项目编号 field0164
     * @return L2 目录段，如 {@code 10%环丙氟虫胺可分散液剂（IPDP-202508-089）}
     */
    public static String formatDirectoryName(String ipdpName, String ipdpProjectNo) {
        return trim(ipdpName) + "（" + trim(ipdpProjectNo) + "）";
    }

    /**
     * 比对 OA 与共享盘项目编号（trim + 忽略大小写）
     *
     * @param oaProjectNo   OA field0164
     * @param diskProjectNo 共享盘 L2 括号内编号
     * @return true 表示一致
     */
    public static boolean matchesProjectNo(String oaProjectNo, String diskProjectNo) {
        if (!StringUtils.hasText(oaProjectNo) || !StringUtils.hasText(diskProjectNo)) {
            return false;
        }
        return normalizeProjectNo(oaProjectNo).equals(normalizeProjectNo(diskProjectNo));
    }

    /**
     * 项目编号规范化（trim + 大写）
     *
     * @param projectNo 原始项目编号
     * @return 规范化后的编号；空输入返回空串
     */
    public static String normalizeProjectNo(String projectNo) {
        if (!StringUtils.hasText(projectNo)) {
            return "";
        }
        return projectNo.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
