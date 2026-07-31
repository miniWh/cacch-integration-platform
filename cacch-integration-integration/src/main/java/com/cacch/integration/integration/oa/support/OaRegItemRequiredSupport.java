package com.cacch.integration.integration.oa.support;

import com.cacch.integration.common.constant.oa.ShareDirProvisionConstants;
import org.springframework.util.StringUtils;

/**
 * OA 资料列表「需要 / 不需要」字段归一化（field0216：0=需要，1=不需要）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaRegItemRequiredSupport {

    private OaRegItemRequiredSupport() {
    }

    /**
     * 判断资料项是否视为「需要」（含空值、0 及未知值，保守视为需要）
     *
     * @param itemRequired OA formson_5464.field0216 原始值；仅 {@code "1"} 为不需要
     * @return true 表示需要该资料（应建目录 / 可参与附件同步）
     */
    public static boolean isRequired(String itemRequired) {
        if (!StringUtils.hasText(itemRequired)) {
            return true;
        }
        return !ShareDirProvisionConstants.ITEM_REQUIRED_NO.equals(itemRequired.trim());
    }

    /**
     * 判断资料项是否显式标记为「不需要」
     *
     * @param itemRequired OA field0216 原始值
     * @return true 表示仅当值为 {@code "1"}
     */
    public static boolean isExplicitlyNotRequired(String itemRequired) {
        return StringUtils.hasText(itemRequired)
                && ShareDirProvisionConstants.ITEM_REQUIRED_NO.equals(itemRequired.trim());
    }
}
