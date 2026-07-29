package com.cacch.integration.integration.sharedrive.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ShareDriveFinalVersionSupport} 单元测试
 */
class ShareDriveFinalVersionSupportTest {

    private static final String SUFFIX = "_最终版本";

    @Test
    void isFinalVersionFileName_matchesSuffixBeforeExtension() {
        assertTrue(ShareDriveFinalVersionSupport.isFinalVersionFileName("表单内容_最终版本.pdf", SUFFIX));
        assertFalse(ShareDriveFinalVersionSupport.isFinalVersionFileName("表单内容.pdf", SUFFIX));
        assertFalse(ShareDriveFinalVersionSupport.isFinalVersionFileName("表单内容_v3.pdf", SUFFIX));
    }

    @Test
    void pickLatestFinalVersion_usesLatestCreatedAt() {
        LocalDateTime older = LocalDateTime.of(2026, 7, 29, 10, 0, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 7, 29, 11, 0, 0);
        ShareDriveFinalVersionSupport.CandidateFile oldFile = candidate("旧_最终版本.pdf", older);
        ShareDriveFinalVersionSupport.CandidateFile newFile = candidate("新_最终版本.pdf", newer);
        ShareDriveFinalVersionSupport.CandidateFile latest =
                ShareDriveFinalVersionSupport.pickLatestFinalVersion(List.of(oldFile, newFile), SUFFIX);
        assertNotNull(latest);
        assertEquals("新_最终版本.pdf", latest.fileName());
    }

    @Test
    void pickLatestFinalVersion_returnsNullWhenNoFinalVersionFile() {
        ShareDriveFinalVersionSupport.CandidateFile plain = candidate("表单内容.pdf",
                LocalDateTime.of(2026, 7, 29, 10, 0, 0));
        assertNull(ShareDriveFinalVersionSupport.pickLatestFinalVersion(List.of(plain), SUFFIX));
    }

    private static ShareDriveFinalVersionSupport.CandidateFile candidate(String fileName, LocalDateTime createdAt) {
        return new ShareDriveFinalVersionSupport.CandidateFile(
                fileName, 1024L, createdAt, createdAt, "application/pdf");
    }
}
