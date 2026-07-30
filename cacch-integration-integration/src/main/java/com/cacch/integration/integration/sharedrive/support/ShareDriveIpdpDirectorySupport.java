package com.cacch.integration.integration.sharedrive.support;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 共享盘 L2 目录解析：{@code IPDP名称（项目编号）}，括号中英文均可
 *
 * <p>项目编号取值以 OA {@code formmain_4070.field0164} 为准；磁盘 L2 括号内为路径定位段，
 * 同步入库与幂等键均使用 OA 侧 field0164，不限制编号格式。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class ShareDriveIpdpDirectorySupport {

    private static final Pattern PAREN_SEGMENT = Pattern.compile("[（(]([^）)]+)[）)]");

    private ShareDriveIpdpDirectorySupport() {
    }

    /**
     * L2 目录解析结果
     *
     * @param directoryName 磁盘 L2 目录原名
     * @param ipdpName      IPDP 名称（括号外，对应 OA field0160 归一化）
     * @param ipdpProjectNo 项目编号（L2 最后一对括号内，用于路径消歧；入库以 OA field0164 为准）
     */
    public record ParsedIpdpDirectory(String directoryName, String ipdpName, String ipdpProjectNo) {
    }

    private record ParenGroup(int start, int end, String content) {
    }

    /**
     * 从 L2 目录名解析 IPDP 名称与括号内项目编号段（取最后一对括号）
     *
     * @param directoryName 共享盘 L2 目录名
     * @return 解析结果；格式不符合时返回 null
     */
    public static ParsedIpdpDirectory parse(String directoryName) {
        if (!StringUtils.hasText(directoryName)) {
            return null;
        }
        String trimmed = directoryName.trim();
        List<ParenGroup> groups = findAllParenGroups(trimmed);
        if (groups.isEmpty()) {
            return null;
        }
        ParenGroup last = groups.get(groups.size() - 1);
        String ipdpProjectNo = last.content().trim();
        if (!StringUtils.hasText(ipdpProjectNo)) {
            return null;
        }
        String ipdpName = normalizeIpdpNameForMatch(trimmed.substring(0, last.start()));
        if (!StringUtils.hasText(ipdpName)) {
            return null;
        }
        return new ParsedIpdpDirectory(trimmed, ipdpName, ipdpProjectNo);
    }

    /**
     * 归一化 IPDP 名称用于匹配与入库（剔除 field0160 中括号段，项目编号以 field0164 为准）
     *
     * @param ipdpName OA field0160 或磁盘解析出的 IPDP 名称
     * @return 去掉全部括号段后的名称
     */
    public static String normalizeIpdpNameForMatch(String ipdpName) {
        if (!StringUtils.hasText(ipdpName)) {
            return "";
        }
        return PAREN_SEGMENT.matcher(ipdpName.trim()).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    /**
     * 按 OA 字段拼接 L2 目录名（默认中文括号；项目编号取自 field0164 原文）
     *
     * @param ipdpName      IPDP 名称 field0160
     * @param ipdpProjectNo 项目编号 field0164
     * @return L2 目录段
     */
    public static String formatDirectoryName(String ipdpName, String ipdpProjectNo) {
        return normalizeIpdpNameForMatch(ipdpName) + "（" + trim(ipdpProjectNo) + "）";
    }

    /**
     * 比对 OA field0164 与共享盘 L2 括号内编号（trim + 忽略大小写，不限制格式）
     *
     * @param oaProjectNo   OA field0164
     * @param diskProjectNo 共享盘 L2 括号内文本
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

    private static List<ParenGroup> findAllParenGroups(String text) {
        List<ParenGroup> groups = new ArrayList<>();
        Matcher matcher = PAREN_SEGMENT.matcher(text);
        while (matcher.find()) {
            groups.add(new ParenGroup(matcher.start(), matcher.end(), matcher.group(1)));
        }
        return groups;
    }

    private static String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
