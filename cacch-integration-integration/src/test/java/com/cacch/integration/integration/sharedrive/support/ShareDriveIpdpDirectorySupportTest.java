package com.cacch.integration.integration.sharedrive.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ShareDriveIpdpDirectorySupport} 单元测试
 */
class ShareDriveIpdpDirectorySupportTest {

    @Test
    void parse_supportsChineseParentheses() {
        ShareDriveIpdpDirectorySupport.ParsedIpdpDirectory parsed = ShareDriveIpdpDirectorySupport.parse(
                "10%环丙氟虫胺可分散液剂（IPDP-202508-089）");
        assertNotNull(parsed);
        assertEquals("10%环丙氟虫胺可分散液剂", parsed.ipdpName());
        assertEquals("IPDP-202508-089", parsed.ipdpProjectNo());
    }

    @Test
    void parse_supportsEnglishParentheses() {
        ShareDriveIpdpDirectorySupport.ParsedIpdpDirectory parsed = ShareDriveIpdpDirectorySupport.parse(
                "10%环丙氟虫胺可分散液剂(IPDP-202508-089)");
        assertNotNull(parsed);
        assertEquals("10%环丙氟虫胺可分散液剂", parsed.ipdpName());
        assertEquals("IPDP-202508-089", parsed.ipdpProjectNo());
    }

    @Test
    void parse_usesLastParenthesesAsProjectNoSegment() {
        ShareDriveIpdpDirectorySupport.ParsedIpdpDirectory parsed = ShareDriveIpdpDirectorySupport.parse(
                "21%环丙氟虫胺·螺虫乙酯（6+15）可分散液剂（IPDP-202501-010）");
        assertNotNull(parsed);
        assertEquals("21%环丙氟虫胺·螺虫乙酯可分散液剂", parsed.ipdpName());
        assertEquals("IPDP-202501-010", parsed.ipdpProjectNo());
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
    void parse_returnsNullWhenMissingProjectNo() {
        assertNull(ShareDriveIpdpDirectorySupport.parse("10%环丙氟虫胺可分散液剂"));
    }

    @Test
    void normalizeIpdpNameForMatch_stripsFormulationParentheses() {
        assertEquals("21%环丙氟虫胺·螺虫乙酯可分散液剂",
                ShareDriveIpdpDirectorySupport.normalizeIpdpNameForMatch(
                        "21%环丙氟虫胺·螺虫乙酯可分散液剂（6+15）"));
    }

    @Test
    void formatDirectoryName_usesField0164Value() {
        assertEquals("10%环丙氟虫胺可分散液剂（IPDP-202508-089）",
                ShareDriveIpdpDirectorySupport.formatDirectoryName(
                        "10%环丙氟虫胺可分散液剂", "IPDP-202508-089"));
    }

    @Test
    void matchesProjectNo_ignoresCaseAndFormat() {
        assertTrue(ShareDriveIpdpDirectorySupport.matchesProjectNo("ipdp-202508-089", "IPDP-202508-089"));
        assertTrue(ShareDriveIpdpDirectorySupport.matchesProjectNo("6+15", "6+15"));
    }
}
