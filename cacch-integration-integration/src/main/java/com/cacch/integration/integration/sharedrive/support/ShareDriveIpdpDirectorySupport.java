package com.cacch.integration.integration.sharedrive.support;

import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 共享盘 L2 目录解析：{@code IPDP名称（项目编号）}，括号中英文均可
 *
 * <p>格式：{@code {IPDP名称，可含括号}（{field0164 项目编号}）}，项目编号取<strong>最后一对</strong>括号；
 * 扫描 L2 时即按 field0164 与 OA 预加载索引匹配，再进入 L3 读文件。</p>
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
     * @param ipdpName      IPDP 名称段（最后一对括号前的原文，可含配方括号如 {@code (6+15)}）
     * @param ipdpProjectNo 项目编号（L2 最后一对括号内，对应 OA field0164）
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
        String ipdpName = trimmed.substring(0, last.start()).trim();
        if (!StringUtils.hasText(ipdpName)) {
            return null;
        }
        return new ParsedIpdpDirectory(trimmed, ipdpName, ipdpProjectNo);
    }

    /**
     * 判断磁盘 L2 项目编号是否在 OA field0164 允许集合内
     *
     * @param allowedNormalizedProjectNos OA field0164 规范化集合；空表示不过滤
     * @param diskProjectNo               L2 最后一对括号内文本
     * @return true 表示允许扫描该 L2
     */
    public static boolean matchesAllowedProjectNo(Set<String> allowedNormalizedProjectNos, String diskProjectNo) {
        if (allowedNormalizedProjectNos == null || allowedNormalizedProjectNos.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(diskProjectNo)) {
            return false;
        }
        return allowedNormalizedProjectNos.contains(normalizeProjectNo(diskProjectNo));
    }

    /**
     * 按磁盘负责人目录名解析 OA 预加载的项目编号集合
     *
     * @param ownerIndex    负责人 → field0164 集合（key 为 OA 登记负责人姓名）
     * @param diskOwnerName 共享盘 L1 目录名
     * @return 允许的项目编号集合；无匹配负责人时返回空集合
     */
    public static Set<String> resolveAllowedProjectNos(Map<String, Set<String>> ownerIndex, String diskOwnerName) {
        if (ownerIndex == null || ownerIndex.isEmpty() || !StringUtils.hasText(diskOwnerName)) {
            return Set.of();
        }
        Set<String> exact = ownerIndex.get(diskOwnerName.trim());
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Set<String>> entry : ownerIndex.entrySet()) {
            if (ShareDrivePathNormalizer.matchesDirectoryNameLoosely(diskOwnerName, entry.getKey())) {
                return entry.getValue();
            }
        }
        return Set.of();
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
