package com.cacch.integration.integration.oa.support;

import com.cacch.integration.integration.oa.client.dto.OaRegReportItemRow;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 共享盘目录与 OA 资料列表行匹配
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaRegReportItemMatcher {

    private OaRegReportItemMatcher() {
    }

    /**
     * 按共享盘三级目录名反查 OA 资料行
     *
     * @param candidates 候选 OA 资料行（须已按负责人/主表预过滤）
     * @param scanned    共享盘扫描结果
     * @param formMainId 可选主表 ID；非空时仅匹配该主表
     * @return 匹配的 OA 行；无匹配或存在歧义匹配时返回 null
     */
    public static OaRegReportItemRow match(List<OaRegReportItemRow> candidates,
                                           ShareDriveScannedItem scanned,
                                           Long formMainId) {
        if (candidates == null || candidates.isEmpty() || scanned == null) {
            return null;
        }
        OaRegReportItemRow matched = null;
        for (OaRegReportItemRow row : candidates) {
            if (formMainId != null && formMainId > 0 && !formMainId.equals(row.formMainId())) {
                continue;
            }
            if (!matchesOwner(row.ownerName(), scanned.ownerName())) {
                continue;
            }
            if (!ShareDrivePathNormalizer.matchesDirectoryNameLoosely(row.ipdpName(), scanned.ipdpName())) {
                continue;
            }
            if (!ShareDrivePathNormalizer.matchesDirectoryNameLoosely(row.itemName(), scanned.itemName())) {
                continue;
            }
            if (matched != null && !Objects.equals(matched.subRowId(), row.subRowId())) {
                return null;
            }
            matched = row;
        }
        return matched;
    }

    private static boolean matchesOwner(String oaOwner, String diskOwner) {
        if (!StringUtils.hasText(oaOwner) || !StringUtils.hasText(diskOwner)) {
            return false;
        }
        return ShareDrivePathNormalizer.matchesDirectoryNameLoosely(oaOwner, diskOwner);
    }
}
