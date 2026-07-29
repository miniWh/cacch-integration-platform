package com.cacch.integration.integration.sharedrive.support;

import com.cacch.integration.common.constant.sharedrive.ShareDriveConstants;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.share.DiskShare;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 共享盘目录解析（OA 字段与磁盘实际文件夹名可能仅有全半角等细微差异）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
public final class ShareDriveDirectorySupport {

    private static final String BIZ = ShareDriveConstants.LOG_BIZ;

    private static final long FILE_ATTRIBUTE_DIRECTORY = 0x00000010L;

    private static final int MIN_PREFIX_MATCH_LEN = 4;

    private ShareDriveDirectorySupport() {
    }

    /**
     * 解析 SMB 相对目录：逐级列举父目录匹配（不依赖 folderExists，兼容路径中含 {@code %}）
     *
     * @param share       已连接的共享
     * @param relativeDir 相对共享根的路径，如 {@code 李庆辉\21%环丙…\农药登记申请表}
     * @return 磁盘上实际存在的相对路径；无法解析时返回 null
     */
    public static String resolveRelativeDirectory(DiskShare share, String relativeDir) {
        if (!StringUtils.hasText(relativeDir)) {
            return "";
        }
        String normalized = relativeDir.replace('/', '\\');
        while (normalized.startsWith("\\")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("\\")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!StringUtils.hasText(normalized)) {
            return "";
        }

        String[] segments = normalized.split("\\\\");
        String current = "";
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            boolean allowPrefixMatch = i > 0;
            String matched = findChildDirectory(share, current, segment, allowPrefixMatch);
            if (matched == null) {
                return null;
            }
            current = current.isEmpty() ? matched : current + "\\" + matched;
        }
        return current;
    }

    /**
     * 判断目录条目是否为子目录
     *
     * @param entry 目录项
     * @return true 表示文件夹
     */
    public static boolean isDirectoryEntry(FileIdBothDirectoryInformation entry) {
        return entry != null && (entry.getFileAttributes() & FILE_ATTRIBUTE_DIRECTORY) != 0;
    }

    private static String findChildDirectory(DiskShare share,
                                             String parentDir,
                                             String expectedName,
                                             boolean allowPrefixMatch) {
        String listPath = StringUtils.hasText(parentDir) ? parentDir : "";
        List<String> directoryNames = new ArrayList<>();
        String matched = null;
        for (FileIdBothDirectoryInformation entry : share.list(listPath)) {
            String name = entry.getFileName();
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            if (!isDirectoryEntry(entry)) {
                continue;
            }
            directoryNames.add(name);
            if (matched == null && ShareDrivePathNormalizer.matchesDirectoryNameLoosely(name, expectedName)) {
                matched = name;
            }
        }
        if (matched != null) {
            return matched;
        }
        if (allowPrefixMatch) {
            matched = findPrefixMatch(directoryNames, expectedName);
            if (matched != null) {
                return matched;
            }
        }
        log.info("【{}】SMB 目录段匹配失败, {}", BIZ,
                formatCandidates(listPath, expectedName, directoryNames));
        return null;
    }

    private static String findPrefixMatch(List<String> directoryNames, String expectedName) {
        String expectedKey = ShareDrivePathNormalizer.canonicalForMatch(expectedName);
        if (expectedKey.length() < MIN_PREFIX_MATCH_LEN) {
            return null;
        }
        String best = null;
        int bestLen = 0;
        for (String name : directoryNames) {
            String diskKey = ShareDrivePathNormalizer.canonicalForMatch(name);
            if (diskKey.length() < MIN_PREFIX_MATCH_LEN) {
                continue;
            }
            if (diskKey.startsWith(expectedKey) || expectedKey.startsWith(diskKey)) {
                int len = Math.min(diskKey.length(), expectedKey.length());
                if (len > bestLen) {
                    bestLen = len;
                    best = name;
                }
            }
        }
        return best;
    }

    /**
     * 目录段匹配失败时输出候选目录（供日志调用）
     *
     * @param parentDir      父目录相对路径
     * @param expectedName   期望目录名
     * @param directoryNames 父目录下全部子文件夹名
     * @return 格式化后的候选列表描述
     */
    public static String formatCandidates(String parentDir, String expectedName, List<String> directoryNames) {
        return "parent=" + (StringUtils.hasText(parentDir) ? parentDir : "(root)")
                + ", expected=" + expectedName
                + ", candidates=" + directoryNames;
    }
}
