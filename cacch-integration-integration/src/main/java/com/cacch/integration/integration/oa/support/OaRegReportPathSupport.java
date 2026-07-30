package com.cacch.integration.integration.oa.support;

import com.cacch.integration.common.config.sharedrive.ShareDriveProperties;
import com.cacch.integration.integration.sharedrive.support.ShareDriveIpdpDirectorySupport;
import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import org.springframework.util.StringUtils;

/**
 * 国内登记报告共享盘路径拼接
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaRegReportPathSupport {

    private OaRegReportPathSupport() {
    }

    /**
     * 拼接资料项目三级目录路径（与 OA field 原文及共享盘文件夹名保持一致，仅 trim）
     *
     * @param rootPath  共享盘根路径
     * @param ownerName 登记负责人（OA 原文）
     * @param ipdpDirectoryName IPDP 目录段（磁盘 L2 原名，含项目编号括号）
     * @param itemName          资料项目名称（OA 原文）
     * @return UNC 目录路径
     */
    public static String buildItemDirectory(String rootPath,
                                            String ownerName,
                                            String ipdpDirectoryName,
                                            String itemName) {
        String normalizedRoot = normalizeRoot(rootPath);
        return normalizedRoot
                + trimSegment(ownerName) + "\\"
                + trimSegment(ipdpDirectoryName) + "\\"
                + trimSegment(itemName);
    }

    /**
     * 按 OA 字段拼接资料项目三级目录路径（L2 默认中文括号）
     *
     * @param rootPath      共享盘根路径
     * @param ownerName     登记负责人（OA 原文）
     * @param ipdpName      IPDP 名称（OA field0160）
     * @param ipdpProjectNo IPDP 项目编号（OA field0164）
     * @param itemName      资料项目名称（OA 原文）
     * @return UNC 目录路径
     */
    public static String buildItemDirectoryFromOaFields(String rootPath,
                                                        String ownerName,
                                                        String ipdpName,
                                                        String ipdpProjectNo,
                                                        String itemName) {
        String ipdpDirectoryName = ShareDriveIpdpDirectorySupport.formatDirectoryName(ipdpName, ipdpProjectNo);
        return buildItemDirectory(rootPath, ownerName, ipdpDirectoryName, itemName);
    }

    private static String trimSegment(String segment) {
        return segment != null ? segment.trim() : "";
    }

    /**
     * 构建路径匹配键（负责人 + 归一化 IPDP 名称 + 项目编号），用于检测删字符后撞名
     *
     * @param ownerName     登记负责人 OA 原文
     * @param ipdpName      IPDP 名称 OA 原文
     * @param ipdpProjectNo IPDP 项目编号 OA 原文
     * @return 匹配键
     */
    public static String buildNormalizedIpdpKey(String ownerName, String ipdpName, String ipdpProjectNo) {
        return ShareDrivePathNormalizer.normalize(ownerName)
                + "|" + ShareDrivePathNormalizer.normalize(ipdpName)
                + "|" + ShareDriveIpdpDirectorySupport.normalizeProjectNo(ipdpProjectNo);
    }

    /**
     * 从配置校验根路径
     *
     * @param properties 共享盘配置
     * @return 规范化根路径；未配置时返回空串
     */
    public static String resolveRootPath(ShareDriveProperties properties) {
        if (properties == null || !properties.isConfigured()) {
            return "";
        }
        return normalizeRoot(properties.getRootPath());
    }

    private static String normalizeRoot(String rootPath) {
        if (!StringUtils.hasText(rootPath)) {
            return "";
        }
        String trimmed = rootPath.trim();
        if (trimmed.endsWith("\\") || trimmed.endsWith("/")) {
            return trimmed;
        }
        return trimmed + "\\";
    }
}
