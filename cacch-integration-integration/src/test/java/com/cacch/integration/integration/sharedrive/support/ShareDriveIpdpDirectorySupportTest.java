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
    void parse_returnsNullWhenMissingProjectNo() {
        assertNull(ShareDriveIpdpDirectorySupport.parse("10%环丙氟虫胺可分散液剂"));
    }

    @Test
    void formatDirectoryName_usesChineseParenthesesByDefault() {
        assertEquals("10%环丙氟虫胺可分散液剂（IPDP-202508-089）",
                ShareDriveIpdpDirectorySupport.formatDirectoryName(
                        "10%环丙氟虫胺可分散液剂", "IPDP-202508-089"));
    }

    @Test
    void matchesProjectNo_ignoresCase() {
        assertTrue(ShareDriveIpdpDirectorySupport.matchesProjectNo("ipdp-202508-089", "IPDP-202508-089"));
    }
}
