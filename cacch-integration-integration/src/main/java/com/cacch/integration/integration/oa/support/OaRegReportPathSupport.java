package com.cacch.integration.integration.oa.support;

import com.cacch.integration.common.config.sharedrive.ShareDriveProperties;
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
     * 拼接资料项目三级目录路径（OA 名称经共享盘归一化后匹配实际目录）
     *
     * @param rootPath  共享盘根路径
     * @param ownerName 登记负责人（OA 原文）
     * @param ipdpName  IPDP 名称（OA 原文）
     * @param itemName  资料项目名称（OA 原文）
     * @return UNC 目录路径
     */
    public static String buildItemDirectory(String rootPath,
                                            String ownerName,
                                            String ipdpName,
                                            String itemName) {
        String normalizedRoot = normalizeRoot(rootPath);
        return normalizedRoot
                + ShareDrivePathNormalizer.normalize(ownerName) + "\\"
                + ShareDrivePathNormalizer.normalize(ipdpName) + "\\"
                + ShareDrivePathNormalizer.normalize(itemName);
    }

    /**
     * 构建路径匹配键（负责人 + 归一化 IPDP），用于检测删字符后撞名
     *
     * @param ownerName 登记负责人 OA 原文
     * @param ipdpName  IPDP OA 原文
     * @return 匹配键
     */
    public static String buildNormalizedIpdpKey(String ownerName, String ipdpName) {
        return ShareDrivePathNormalizer.normalize(ownerName) + "|" + ShareDrivePathNormalizer.normalize(ipdpName);
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
