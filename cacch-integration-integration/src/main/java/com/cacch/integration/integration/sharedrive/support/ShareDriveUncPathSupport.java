package com.cacch.integration.integration.sharedrive.support;

import org.springframework.util.StringUtils;

/**
 * UNC 共享盘路径解析
 *
 * @author hongfu_zhou@cacch.com
 */
public final class ShareDriveUncPathSupport {

    private ShareDriveUncPathSupport() {
    }

    /**
     * UNC 根路径（\\host\share）
     *
     * @param host      主机名或 IP
     * @param shareName 共享名
     */
    public record UncRoot(String host, String shareName) {
    }

    /**
     * 解析 UNC 根路径
     *
     * @param rootPath 如 {@code \\192.168.1.8\国内登记资料}
     * @return 解析结果；格式非法时返回 null
     */
    public static UncRoot parseRoot(String rootPath) {
        if (!StringUtils.hasText(rootPath)) {
            return null;
        }
        String normalized = rootPath.trim().replace('/', '\\');
        while (normalized.endsWith("\\")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.startsWith("\\\\")) {
            return null;
        }
        String withoutPrefix = normalized.substring(2);
        int slash = withoutPrefix.indexOf('\\');
        if (slash <= 0 || slash >= withoutPrefix.length() - 1) {
            return null;
        }
        String host = withoutPrefix.substring(0, slash);
        String share = withoutPrefix.substring(slash + 1);
        if (!StringUtils.hasText(host) || !StringUtils.hasText(share)) {
            return null;
        }
        return new UncRoot(host, share);
    }

    /**
     * 从完整 UNC 目录路径提取相对共享根的路径（使用反斜杠）
     *
     * @param directoryPath 完整目录 UNC
     * @param rootPath      共享根 UNC
     * @return 相对路径，如 {@code 杨燕玲\10 环丙氟虫胺\农药登记变更申请表}；无法解析时返回 null
     */
    public static String toRelativeDirectory(String directoryPath, String rootPath) {
        if (!StringUtils.hasText(directoryPath) || !StringUtils.hasText(rootPath)) {
            return null;
        }
        String dir = directoryPath.trim().replace('/', '\\');
        String root = rootPath.trim().replace('/', '\\');
        while (dir.endsWith("\\")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        while (root.endsWith("\\")) {
            root = root.substring(0, root.length() - 1);
        }
        if (!dir.regionMatches(true, 0, root, 0, root.length())) {
            return null;
        }
        if (dir.length() == root.length()) {
            return "";
        }
        if (dir.charAt(root.length()) != '\\') {
            return null;
        }
        return dir.substring(root.length() + 1);
    }

    /**
     * 是否为 UNC 网络路径（{@code \\host\share} 或 {@code //host/share}）
     *
     * @param path 路径
     * @return true 表示远程 SMB 共享路径
     */
    public static boolean isUncPath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        String trimmed = path.trim();
        return trimmed.startsWith("\\\\") || trimmed.startsWith("//");
    }
}
