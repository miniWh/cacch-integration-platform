package com.cacch.integration.integration.sharedrive.support;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 共享盘「最终版本」文件名筛选与选取
 *
 * @author hongfu_zhou@cacch.com
 */
public final class ShareDriveFinalVersionSupport {

    private ShareDriveFinalVersionSupport() {
    }

    /**
     * 目录内候选文件元数据（不含内容）
     *
     * @param fileName    文件名
     * @param fileSize    大小（字节）
     * @param createdAt   创建时间
     * @param modifiedAt  最后修改时间
     * @param contentType MIME 类型
     */
    public record CandidateFile(
            String fileName,
            long fileSize,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt,
            String contentType
    ) {
    }

    /**
     * 判断文件名（不含扩展名）是否以最终版本后缀结尾
     *
     * @param fileName 文件名
     * @param suffix   最终版本后缀，如 {@code _最终版本}
     * @return true 表示符合上传条件
     */
    public static boolean isFinalVersionFileName(String fileName, String suffix) {
        if (!StringUtils.hasText(fileName) || !StringUtils.hasText(suffix)) {
            return false;
        }
        String trimmed = fileName.trim();
        int dot = trimmed.lastIndexOf('.');
        String baseName = dot > 0 ? trimmed.substring(0, dot) : trimmed;
        return baseName.endsWith(suffix.trim());
    }

    /**
     * 从候选文件中选取创建时间最新的「最终版本」文件
     *
     * @param candidates 目录内全部候选文件元数据
     * @param suffix     最终版本后缀
     * @return 最新最终版本文件；无匹配时返回 null
     */
    public static CandidateFile pickLatestFinalVersion(List<CandidateFile> candidates, String suffix) {
        if (candidates == null || candidates.isEmpty() || !StringUtils.hasText(suffix)) {
            return null;
        }
        return candidates.stream()
                .filter(candidate -> isFinalVersionFileName(candidate.fileName(), suffix))
                .max(Comparator
                        .comparing(CandidateFile::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(CandidateFile::modifiedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(CandidateFile::fileName, Comparator.nullsFirst(String::compareToIgnoreCase)))
                .orElse(null);
    }
}
