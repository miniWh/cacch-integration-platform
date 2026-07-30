package com.cacch.integration.integration.oa.support;

import com.cacch.integration.integration.oa.client.dto.OaRegReportItemRow;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link OaRegReportItemMatcher} 单元测试
 */
class OaRegReportItemMatcherTest {

    private static final String IPDP_DIR = "10%环丙氟虫胺可分散液剂（IPDP-202508-089）";
    private static final String IPDP_NAME = "10%环丙氟虫胺可分散液剂";
    private static final String PROJECT_NO = "IPDP-202508-089";

    @Test
    void match_usesOaField0164WhenDiskProjectNoDiffersAndSingleCandidate() {
        ShareDriveScannedItem scanned = new ShareDriveScannedItem(
                "李庆辉",
                "21%环丙氟虫胺·螺虫乙酯可分散液剂（6+15）",
                "21%环丙氟虫胺·螺虫乙酯可分散液剂",
                "6+15",
                "农药登记申请表",
                "\\\\server\\root\\李庆辉\\21%环丙氟虫胺·螺虫乙酯可分散液剂（6+15）\\农药登记申请表",
                null);

        List<OaRegReportItemRow> candidates = List.of(
                row("8323386097039524452", "李庆辉",
                        "21%环丙氟虫胺·螺虫乙酯可分散液剂（6+15）", "IPDP-202501-010", "11", "农药登记申请表"));

        OaRegReportItemRow matched = OaRegReportItemMatcher.match(candidates, scanned, null);
        assertNotNull(matched);
        assertEquals("IPDP-202501-010", matched.ipdpProjectNo());
    }

    @Test
    void match_matchesWhenOaIpdpNameContainsFormulationParentheses() {
        ShareDriveScannedItem scanned = new ShareDriveScannedItem(
                "李庆辉",
                "21%环丙氟虫胺·螺虫乙酯可分散液剂（IPDP-202501-010）",
                "21%环丙氟虫胺·螺虫乙酯可分散液剂",
                "IPDP-202501-010",
                "农药登记申请表",
                "\\\\server\\root\\李庆辉\\21%环丙氟虫胺·螺虫乙酯可分散液剂（IPDP-202501-010）\\农药登记申请表",
                null);

        List<OaRegReportItemRow> candidates = List.of(
                row("8323386097039524452", "李庆辉",
                        "21%环丙氟虫胺·螺虫乙酯可分散液剂（6+15）", "IPDP-202501-010", "11", "农药登记申请表"));

        OaRegReportItemRow matched = OaRegReportItemMatcher.match(candidates, scanned, null);
        assertNotNull(matched);
        assertEquals("8323386097039524452", matched.formMainId());
    }

    @Test
    void match_prefersExactIpdpWhenLooseMatchesMultipleForms() {
        ShareDriveScannedItem scanned = scanned("杨燕玲", "产品概述");

        List<OaRegReportItemRow> candidates = List.of(
                row("1", "杨燕玲", "10%环丙氟虫胺可分散液剂", PROJECT_NO, "11", "产品概述"),
                row("2", "杨燕玲", "环丙氟虫胺可分散液剂", PROJECT_NO, "22", "产品概述"));

        OaRegReportItemRow matched = OaRegReportItemMatcher.match(candidates, scanned, null);
        assertNotNull(matched);
        assertEquals("1", matched.formMainId());
        assertEquals("11", matched.subRowId());
    }

    @Test
    void match_distinguishesSameIpdpNameByProjectNo() {
        ShareDriveScannedItem scanned = scanned("杨燕玲", "产品概述");

        List<OaRegReportItemRow> candidates = List.of(
                row("1", "杨燕玲", IPDP_NAME, "IPDP-202508-089", "11", "产品概述"),
                row("2", "杨燕玲", IPDP_NAME, "IPDP-202508-090", "22", "产品概述"));

        OaRegReportItemRow matched = OaRegReportItemMatcher.match(candidates, scanned, null);
        assertNotNull(matched);
        assertEquals("1", matched.formMainId());
    }

    @Test
    void match_usesHintFormMainIdsToDisambiguate() {
        ShareDriveScannedItem scanned = scanned("杨燕玲", "产品概述");

        List<OaRegReportItemRow> candidates = List.of(
                row("100", "杨燕玲", IPDP_NAME, PROJECT_NO, "11", "产品概述"),
                row("200", "杨燕玲", IPDP_NAME, PROJECT_NO, "22", "产品概述"));

        OaRegReportItemRow matched = OaRegReportItemMatcher.match(
                candidates, scanned, null, List.of("200"));
        assertNotNull(matched);
        assertEquals("200", matched.formMainId());
    }

    @Test
    void match_returnsNullWhenMultipleExactMatchesWithoutHint() {
        ShareDriveScannedItem scanned = scanned("杨燕玲", "产品概述");

        List<OaRegReportItemRow> candidates = List.of(
                row("100", "杨燕玲", IPDP_NAME, PROJECT_NO, "11", "产品概述"),
                row("200", "杨燕玲", IPDP_NAME, PROJECT_NO, "22", "产品概述"));

        assertNull(OaRegReportItemMatcher.match(candidates, scanned, null));
    }

    @Test
    void match_returnsNullWhenOwnerAndPathDoNotOverlap() {
        ShareDriveScannedItem scanned = scanned("杨燕玲", "产品概述");

        List<OaRegReportItemRow> candidates = List.of(
                row("1", "张三", IPDP_NAME, PROJECT_NO, "11", "产品概述"));

        assertNull(OaRegReportItemMatcher.match(candidates, scanned, null));
    }

    private static ShareDriveScannedItem scanned(String ownerName, String itemName) {
        return new ShareDriveScannedItem(
                ownerName,
                IPDP_DIR,
                IPDP_NAME,
                PROJECT_NO,
                itemName,
                "\\\\server\\root\\" + ownerName + "\\" + IPDP_DIR + "\\" + itemName,
                null);
    }

    private static OaRegReportItemRow row(String formMainId,
                                          String ownerName,
                                          String ipdpName,
                                          String ipdpProjectNo,
                                          String subRowId,
                                          String itemName) {
        return new OaRegReportItemRow(formMainId, ownerName, ipdpName, ipdpProjectNo, subRowId, itemName, null);
    }
}
