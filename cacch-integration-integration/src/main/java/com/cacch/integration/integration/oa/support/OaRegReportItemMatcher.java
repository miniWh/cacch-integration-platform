package com.cacch.integration.integration.oa.support;

import com.cacch.integration.integration.oa.client.dto.OaRegReportItemRow;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
import com.cacch.integration.integration.sharedrive.support.ShareDriveIpdpDirectorySupport;
import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * @return 匹配的 OA 行；无匹配时返回 null
     */
    public static OaRegReportItemRow match(List<OaRegReportItemRow> candidates,
                                           ShareDriveScannedItem scanned,
                                           String formMainId) {
        return match(candidates, scanned, formMainId, List.of());
    }

    /**
     * 按共享盘三级目录名反查 OA 资料行（支持主表游标批次消歧）
     *
     * @param candidates       候选 OA 资料行
     * @param scanned          共享盘扫描结果
     * @param formMainId       可选主表 ID；非空时仅匹配该主表
     * @param hintFormMainIds  主表游标批次 ID，用于多条命中时的消歧
     * @return 匹配的 OA 行；无匹配或无法消歧时返回 null
     */
    public static OaRegReportItemRow match(List<OaRegReportItemRow> candidates,
                                           ShareDriveScannedItem scanned,
                                           String formMainId,
                                           List<String> hintFormMainIds) {
        if (candidates == null || candidates.isEmpty() || scanned == null) {
            return null;
        }
        List<OaRegReportItemRow> matchedRows = collectMatchedRows(candidates, scanned, formMainId);
        if (matchedRows.isEmpty()) {
            return null;
        }
        if (matchedRows.size() == 1) {
            return matchedRows.getFirst();
        }
        return disambiguate(matchedRows, scanned, hintFormMainIds);
    }

    /**
     * 判断候选行中是否存在多条不同 subRowId 的歧义匹配
     *
     * @param candidates 候选 OA 资料行
     * @param scanned    共享盘扫描结果
     * @param formMainId 可选主表 ID
     * @return true 表示存在歧义匹配
     */
    public static boolean hasAmbiguousMatch(List<OaRegReportItemRow> candidates,
                                            ShareDriveScannedItem scanned,
                                            String formMainId) {
        if (candidates == null || candidates.isEmpty() || scanned == null) {
            return false;
        }
        Set<String> subRowIds = new HashSet<>();
        for (OaRegReportItemRow row : candidates) {
            if (matchSingleRow(row, scanned, formMainId)) {
                subRowIds.add(row.subRowId());
            }
        }
        return subRowIds.size() > 1;
    }

    private static List<OaRegReportItemRow> collectMatchedRows(List<OaRegReportItemRow> candidates,
                                                               ShareDriveScannedItem scanned,
                                                               String formMainId) {
        List<OaRegReportItemRow> matchedRows = new ArrayList<>();
        for (OaRegReportItemRow row : candidates) {
            if (matchSingleRow(row, scanned, formMainId)) {
                matchedRows.add(row);
            }
        }
        return matchedRows;
    }

    private static OaRegReportItemRow disambiguate(List<OaRegReportItemRow> matchedRows,
                                                   ShareDriveScannedItem scanned,
                                                   List<String> hintFormMainIds) {
        List<OaRegReportItemRow> pool = filterExactIpdpAndItem(matchedRows, scanned);
        if (pool.size() == 1) {
            return pool.getFirst();
        }
        if (pool.isEmpty()) {
            pool = filterExactIpdp(matchedRows, scanned);
            if (pool.size() == 1) {
                return pool.getFirst();
            }
            if (pool.isEmpty()) {
                pool = matchedRows;
            }
        }

        List<OaRegReportItemRow> hinted = filterByHintFormMainIds(pool, hintFormMainIds);
        if (hinted.size() == 1) {
            return hinted.getFirst();
        }
        if (!hinted.isEmpty()) {
            pool = hinted;
        }

        Set<String> distinctFormMainIds = new HashSet<>();
        for (OaRegReportItemRow row : pool) {
            if (StringUtils.hasText(row.formMainId())) {
                distinctFormMainIds.add(row.formMainId());
            }
        }
        if (distinctFormMainIds.size() == 1) {
            return pool.getFirst();
        }
        return null;
    }

    private static List<OaRegReportItemRow> filterExactIpdpAndItem(List<OaRegReportItemRow> rows,
                                                                   ShareDriveScannedItem scanned) {
        return rows.stream()
                .filter(row -> ShareDrivePathNormalizer.matchesDirectoryName(row.ipdpName(), scanned.ipdpName())
                        && ShareDriveIpdpDirectorySupport.matchesProjectNo(row.ipdpProjectNo(), scanned.ipdpProjectNo())
                        && ShareDrivePathNormalizer.matchesDirectoryName(row.itemName(), scanned.itemName()))
                .toList();
    }

    private static List<OaRegReportItemRow> filterExactIpdp(List<OaRegReportItemRow> rows,
                                                            ShareDriveScannedItem scanned) {
        return rows.stream()
                .filter(row -> ShareDrivePathNormalizer.matchesDirectoryName(row.ipdpName(), scanned.ipdpName())
                        && ShareDriveIpdpDirectorySupport.matchesProjectNo(row.ipdpProjectNo(), scanned.ipdpProjectNo()))
                .toList();
    }

    private static List<OaRegReportItemRow> filterByHintFormMainIds(List<OaRegReportItemRow> rows,
                                                                      List<String> hintFormMainIds) {
        if (hintFormMainIds == null || hintFormMainIds.isEmpty()) {
            return List.of();
        }
        Set<String> hints = new HashSet<>(hintFormMainIds);
        return rows.stream()
                .filter(row -> StringUtils.hasText(row.formMainId()) && hints.contains(row.formMainId()))
                .toList();
    }

    private static boolean matchSingleRow(OaRegReportItemRow row,
                                          ShareDriveScannedItem scanned,
                                          String formMainId) {
        if (StringUtils.hasText(formMainId) && !OaIdSupport.equalsId(formMainId, row.formMainId())) {
            return false;
        }
        return matchesOwner(row.ownerName(), scanned.ownerName())
                && ShareDrivePathNormalizer.matchesIpdpNameLoosely(row.ipdpName(), scanned.ipdpName())
                && ShareDriveIpdpDirectorySupport.matchesProjectNo(row.ipdpProjectNo(), scanned.ipdpProjectNo())
                && ShareDrivePathNormalizer.matchesDirectoryNameLoosely(row.itemName(), scanned.itemName());
    }

    private static boolean matchesOwner(String oaOwner, String diskOwner) {
        if (!StringUtils.hasText(oaOwner) || !StringUtils.hasText(diskOwner)) {
            return false;
        }
        return ShareDrivePathNormalizer.matchesDirectoryNameLoosely(oaOwner, diskOwner);
    }
}
