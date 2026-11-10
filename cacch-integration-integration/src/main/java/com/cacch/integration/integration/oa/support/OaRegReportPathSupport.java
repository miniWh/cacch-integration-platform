package com.cacch.integration.integration.oa.support;

import com.cacch.integration.common.constant.oa.OaRegReportConstants;
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
     * 格式化 L3 目录名：{@code {field0212序号}、{field0214资料项目名称}}
     *
     * @param itemSeq  资料项目序号（OA field0212）
     * @param itemName 资料项目名称（OA field0214）
     * @return L3 目录段，如 {@code 1、农药登记申请表}
     */
    public static String formatL3DirectoryName(String itemSeq, String itemName) {
        String seq = itemSeq != null ? itemSeq.trim() : "";
        String name = itemName != null ? itemName.trim() : "";
        if (!StringUtils.hasText(seq)) {
            return name;
        }
        if (!StringUtils.hasText(name)) {
            return seq;
        }
        return seq + OaRegReportConstants.L3_ITEM_SEQ_SEPARATOR + name;
    }

    /**
     * 拼接资料项目三级目录路径（L3 段已格式化）
     *
     * @param rootPath          共享盘根路径
     * @param ownerName         登记负责人（OA 原文）
     * @param ipdpDirectoryName IPDP 目录段（磁盘 L2 原名，含项目编号括号）
     * @param l3DirectoryName   L3 目录段（含序号前缀）
     * @return UNC 目录路径
     */
    public static String buildItemDirectory(String rootPath,
                                            String ownerName,
                                            String ipdpDirectoryName,
                                            String l3DirectoryName) {
        String normalizedRoot = normalizeRoot(rootPath);
        return normalizedRoot
                + trimSegment(ownerName) + "\\"
                + trimSegment(ipdpDirectoryName) + "\\"
                + trimSegment(l3DirectoryName);
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
