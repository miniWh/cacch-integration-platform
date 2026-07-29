package com.cacch.integration.integration.sharedrive.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link ShareDriveVersionSupport} 单元测试
 */
class ShareDriveVersionSupportTest {

    @Test
    void pickLatest_acceptsFileWithoutVersionSuffix() {
        Pattern pattern = Pattern.compile("_v(\\d+)");
        ShareDriveVersionSupport.CandidateFile file = new ShareDriveVersionSupport.CandidateFile(
                "表单内容.pdf", 1, 1024L, null, new byte[] {1}, "application/pdf");
        ShareDriveVersionSupport.CandidateFile latest =
                ShareDriveVersionSupport.pickLatest(List.of(file), pattern);
        assertNotNull(latest);
        assertEquals("表单内容.pdf", latest.fileName());
        assertEquals(1, latest.fileVersion());
    }
}
