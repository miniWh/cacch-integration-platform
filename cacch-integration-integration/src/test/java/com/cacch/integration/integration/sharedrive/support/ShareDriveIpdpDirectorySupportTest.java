package com.cacch.integration.integration.sharedrive.support;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ShareDriveIpdpDirectorySupport} 单元测试
 */
class ShareDriveIpdpDirectorySupportTest {

    private static final String L2_WITH_FORMULATION = "21%环丙氟虫胺·螺虫乙酯可分散液剂 (6+15)（IPDP-202605-107）";

    @Test
    void parse_supportsChineseParentheses() {
        ShareDriveIpdpDirectorySupport.ParsedIpdpDirectory parsed = ShareDriveIpdpDirectorySupport.parse(
                "10%环丙氟虫胺可分散液剂（IPDP-202508-089）");
        assertNotNull(parsed);
        assertEquals("10%环丙氟虫胺可分散液剂", parsed.ipdpName());
        assertEquals("IPDP-202508-089", parsed.ipdpProjectNo());
    }

    @Test
    void parse_usesLastParenthesesAsProjectNoWhenNameContainsFormulation() {
        ShareDriveIpdpDirectorySupport.ParsedIpdpDirectory parsed = ShareDriveIpdpDirectorySupport.parse(
                L2_WITH_FORMULATION);
        assertNotNull(parsed);
        assertEquals("21%环丙氟虫胺·螺虫乙酯可分散液剂 (6+15)", parsed.ipdpName());
        assertEquals("IPDP-202605-107", parsed.ipdpProjectNo());
    }

    @Test
    void parse_acceptsAnyProjectNoFormatInLastParentheses() {
        ShareDriveIpdpDirectorySupport.ParsedIpdpDirectory parsed = ShareDriveIpdpDirectorySupport.parse(
                "21%环丙氟虫胺·螺虫乙酯可分散液剂（6+15）");
        assertNotNull(parsed);
        assertEquals("21%环丙氟虫胺·螺虫乙酯可分散液剂", parsed.ipdpName());
        assertEquals("6+15", parsed.ipdpProjectNo());
    }

    @Test
    void matchesAllowedProjectNo_comparesField0164AtScanTime() {
        Set<String> allowed = Set.of("IPDP-202605-107");
        assertTrue(ShareDriveIpdpDirectorySupport.matchesAllowedProjectNo(allowed, "IPDP-202605-107"));
        assertTrue(ShareDriveIpdpDirectorySupport.matchesAllowedProjectNo(allowed, "ipdp-202605-107"));
    }

    @Test
    void resolveAllowedProjectNos_matchesOwnerLoosely() {
        Map<String, Set<String>> index = Map.of("李庆辉", Set.of("IPDP-202605-107"));
        Set<String> allowed = ShareDriveIpdpDirectorySupport.resolveAllowedProjectNos(index, "李庆辉");
        assertEquals(1, allowed.size());
        assertTrue(allowed.contains("IPDP-202605-107"));
    }

    @Test
    void parse_returnsNullWhenMissingProjectNo() {
        assertNull(ShareDriveIpdpDirectorySupport.parse("10%环丙氟虫胺可分散液剂"));
    }

    @Test
    void normalizeIpdpNameForMatch_stripsFormulationParentheses() {
        assertEquals("21%环丙氟虫胺·螺虫乙酯可分散液剂",
                ShareDriveIpdpDirectorySupport.normalizeIpdpNameForMatch(
                        "21%环丙氟虫胺·螺虫乙酯可分散液剂（6+15）"));
    }
}
