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

    @Test
    void match_prefersExactIpdpWhenLooseMatchesMultipleForms() {
        ShareDriveScannedItem scanned = new ShareDriveScannedItem(
                "杨燕玲",
                "10%环丙氟虫胺可分散液剂",
                "产品概述",
                "\\\\server\\root\\杨燕玲\\10%环丙氟虫胺可分散液剂\\产品概述",
                null);

        List<OaRegReportItemRow> candidates = List.of(
                row("1", "杨燕玲", "10%环丙氟虫胺可分散液剂", "11", "产品概述"),
                row("2", "杨燕玲", "环丙氟虫胺可分散液剂", "22", "产品概述"));

        OaRegReportItemRow matched = OaRegReportItemMatcher.match(candidates, scanned, null);
        assertNotNull(matched);
        assertEquals("1", matched.formMainId());
        assertEquals("11", matched.subRowId());
    }

    @Test
    void match_usesHintFormMainIdsToDisambiguate() {
        ShareDriveScannedItem scanned = new ShareDriveScannedItem(
                "杨燕玲",
                "10%环丙氟虫胺可分散液剂",
                "产品概述",
                "\\\\server\\root\\杨燕玲\\10%环丙氟虫胺可分散液剂\\产品概述",
                null);

        List<OaRegReportItemRow> candidates = List.of(
                row("100", "杨燕玲", "10%环丙氟虫胺可分散液剂", "11", "产品概述"),
                row("200", "杨燕玲", "10%环丙氟虫胺可分散液剂", "22", "产品概述"));

        OaRegReportItemRow matched = OaRegReportItemMatcher.match(
                candidates, scanned, null, List.of("200"));
        assertNotNull(matched);
        assertEquals("200", matched.formMainId());
    }

    @Test
    void match_returnsNullWhenMultipleExactMatchesWithoutHint() {
        ShareDriveScannedItem scanned = new ShareDriveScannedItem(
                "杨燕玲",
                "10%环丙氟虫胺可分散液剂",
                "产品概述",
                "\\\\server\\root\\杨燕玲\\10%环丙氟虫胺可分散液剂\\产品概述",
                null);

        List<OaRegReportItemRow> candidates = List.of(
                row("100", "杨燕玲", "10%环丙氟虫胺可分散液剂", "11", "产品概述"),
                row("200", "杨燕玲", "10%环丙氟虫胺可分散液剂", "22", "产品概述"));

        assertNull(OaRegReportItemMatcher.match(candidates, scanned, null));
    }

    @Test
    void match_returnsNullWhenOwnerAndPathDoNotOverlap() {
        ShareDriveScannedItem scanned = new ShareDriveScannedItem(
                "杨燕玲",
                "10%环丙氟虫胺可分散液剂",
                "产品概述",
                "\\\\server\\root\\杨燕玲\\10%环丙氟虫胺可分散液剂\\产品概述",
                null);

        List<OaRegReportItemRow> candidates = List.of(
                row("1", "张三", "10%环丙氟虫胺可分散液剂", "11", "产品概述"));

        assertNull(OaRegReportItemMatcher.match(candidates, scanned, null));
    }

    private static OaRegReportItemRow row(String formMainId,
                                          String ownerName,
                                          String ipdpName,
                                          String subRowId,
                                          String itemName) {
        return new OaRegReportItemRow(formMainId, ownerName, ipdpName, subRowId, itemName, null);
    }
}
