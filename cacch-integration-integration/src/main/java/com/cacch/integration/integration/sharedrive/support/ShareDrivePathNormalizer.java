package com.cacch.integration.integration.sharedrive.support;

import com.cacch.integration.common.constant.sharedrive.ShareDriveConstants;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 共享盘目录名归一化：删除 OA 名称中不能出现在目录里的字符，以共享盘实际目录为准匹配
 *
 * @author hongfu_zhou@cacch.com
 */
public final class ShareDrivePathNormalizer {

    private ShareDrivePathNormalizer() {
    }

    /**
     * 将 OA 字段值转为可用于共享盘路径的目录段
     *
     * <p>规则：trim → 删除非法字符 → 合并连续空白 → 去掉首尾空白与末尾点号。</p>
     *
     * @param oaSegment OA 原始名称（负责人 / IPDP / 资料项目）
     * @return 归一化目录段；输入为空时返回空串
     */
    public static String normalize(String oaSegment) {
        if (!StringUtils.hasText(oaSegment)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char ch : oaSegment.trim().toCharArray()) {
            if (ShareDriveConstants.FORBIDDEN_DIR_CHARS.indexOf(ch) >= 0) {
                continue;
            }
            builder.append(ch);
        }
        String collapsed = builder.toString().replaceAll("\\s+", " ").trim();
        return stripTrailingDots(collapsed);
    }

    /**
     * 判断共享盘目录名是否与 OA 期望值匹配（精确 / 归一化 / 忽略大小写）
     *
     * @param diskName     磁盘上的目录名
     * @param expectedName OA 字段期望值
     * @return true 表示匹配
     */
    public static boolean matchesDirectoryName(String diskName, String expectedName) {
        if (!StringUtils.hasText(diskName) || !StringUtils.hasText(expectedName)) {
            return false;
        }
        String disk = diskName.trim();
        String expected = expectedName.trim();
        if (disk.equals(expected)) {
            return true;
        }
        if (disk.equalsIgnoreCase(expected)) {
            return true;
        }
        return normalize(disk).equals(normalize(expected));
    }

    /**
     * 目录名匹配用 canonical 形式（NFKC、全半角统一、去空白、小写）
     *
     * @param value 原始目录名
     * @return 匹配键
     */
    public static String canonicalForMatch(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC);
        normalized = normalized
                .replace('（', '(')
                .replace('）', ')')
                .replace('【', '[')
                .replace('】', ']')
                .replace('·', '.')
                .replace('．', '.')
                .replace('\u00B7', '.')
                .replace('\u30FB', '.')
                .replace('＋', '+')
                .replace('％', '%');
        normalized = normalize(normalized);
        normalized = normalized.replaceAll("\\s+", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * 宽松目录名匹配（canonical 相等或互为前缀）
     *
     * @param diskName     磁盘目录名
     * @param expectedName OA 期望值
     * @return true 表示匹配
     */
    public static boolean matchesDirectoryNameLoosely(String diskName, String expectedName) {
        if (matchesDirectoryName(diskName, expectedName)) {
            return true;
        }
        String diskKey = canonicalForMatch(diskName);
        String expectedKey = canonicalForMatch(expectedName);
        if (!StringUtils.hasText(diskKey) || !StringUtils.hasText(expectedKey)) {
            return false;
        }
        if (diskKey.equals(expectedKey)) {
            return true;
        }
        int minLen = Math.min(diskKey.length(), expectedKey.length());
        if (minLen < 4) {
            return false;
        }
        if (diskKey.contains(expectedKey) || expectedKey.contains(diskKey)) {
            return true;
        }
        return diskKey.startsWith(expectedKey) || expectedKey.startsWith(diskKey);
    }

    /**
     * IPDP 名称宽松匹配（忽略 leading 含量前缀如 {@code 10%}，并支持互为子串）
     *
     * @param oaIpdpName   OA field0160 原文
     * @param diskIpdpName 共享盘 L2 目录名
     * @return true 表示 IPDP 匹配
     */
    public static boolean matchesIpdpNameLoosely(String oaIpdpName, String diskIpdpName) {
        if (matchesDirectoryNameLoosely(oaIpdpName, diskIpdpName)) {
            return true;
        }
        String oaCore = stripDosagePrefix(canonicalForMatch(oaIpdpName));
        String diskCore = stripDosagePrefix(canonicalForMatch(diskIpdpName));
        if (!StringUtils.hasText(oaCore) || !StringUtils.hasText(diskCore)) {
            return false;
        }
        if (oaCore.equals(diskCore)) {
            return true;
        }
        int minLen = Math.min(oaCore.length(), diskCore.length());
        if (minLen < 4) {
            return false;
        }
        if (oaCore.contains(diskCore) || diskCore.contains(oaCore)) {
            return true;
        }
        return oaCore.startsWith(diskCore) || diskCore.startsWith(oaCore);
    }

    private static String stripDosagePrefix(String canonicalKey) {
        if (!StringUtils.hasText(canonicalKey)) {
            return "";
        }
        return canonicalKey.replaceFirst("^\\d+[%％]", "");
    }

    private static String stripTrailingDots(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith(".") || result.endsWith(" ")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        return result;
    }
}
