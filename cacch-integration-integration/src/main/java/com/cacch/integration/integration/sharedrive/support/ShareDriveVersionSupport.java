package com.cacch.integration.integration.sharedrive.support;

import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 共享盘文件名版本解析
 *
 * @author hongfu_zhou@cacch.com
 */
public final class ShareDriveVersionSupport {

    private ShareDriveVersionSupport() {
    }

    /**
     * 目录内候选文件
     *
     * @param fileName   文件名
     * @param fileSize   大小（字节）
     * @param modifiedAt 最后修改时间
     * @param content    文件内容
     */
    public record CandidateFile(
            String fileName,
            int fileVersion,
            long fileSize,
            LocalDateTime modifiedAt,
            byte[] content,
            String contentType
    ) {
    }

    /**
     * 从候选文件中选取最新版本
     *
     * @param candidates      候选列表
     * @param versionPattern  版本号正则，须含捕获组 1 为版本数字
     * @return 最新版；无候选时返回 null
     */
    public static CandidateFile pickLatest(List<CandidateFile> candidates, Pattern versionPattern) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .max(Comparator
                        .comparingInt(CandidateFile::fileVersion)
                        .thenComparing(CandidateFile::modifiedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(CandidateFile::fileName, Comparator.nullsFirst(String::compareToIgnoreCase)))
                .orElse(null);
    }

    /**
     * 解析文件名中的版本号
     *
     * @param fileName       文件名
     * @param versionPattern 版本正则
     * @return 版本号；未匹配时返回 1
     */
    public static int parseVersion(String fileName, Pattern versionPattern) {
        if (!StringUtils.hasText(fileName) || versionPattern == null) {
            return 1;
        }
        Matcher matcher = versionPattern.matcher(fileName);
        int max = 0;
        while (matcher.find()) {
            try {
                max = Math.max(max, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                // 忽略非法捕获组
            }
        }
        return max > 0 ? max : 1;
    }

    /**
     * 文件最后修改时间转换
     *
     * @param epochMillis 毫秒时间戳
     * @return 本地时间；无效时返回 null
     */
    public static LocalDateTime toLocalDateTime(long epochMillis) {
        if (epochMillis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
