package com.cacch.integration.integration.sharedrive.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ShareDrivePathNormalizer} 单元测试
 */
class ShareDrivePathNormalizerTest {

    @Test
    void normalize_removesForbiddenCharsAndCollapsesSpaces() {
        assertEquals("10 环丙氟虫胺可分散液剂",
                ShareDrivePathNormalizer.normalize("10%  环丙氟虫胺/可分散液剂"));
    }

    @Test
    void normalize_stripsTrailingDots() {
        assertEquals("资料项目A", ShareDrivePathNormalizer.normalize("资料项目A..."));
    }

    @Test
    void normalize_blankReturnsEmpty() {
        assertEquals("", ShareDrivePathNormalizer.normalize("   "));
    }
}
