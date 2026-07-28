package com.cacch.integration.integration.sharedrive.support;

import com.cacch.integration.common.constant.sharedrive.ShareDriveConstants;
import org.springframework.util.StringUtils;

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
