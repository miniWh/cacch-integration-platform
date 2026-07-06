package com.cacch.integration.common.dto.meeting;

/**
 * 扫描会议管理子表并尝试建会的统计结果
 *
 * @author hongfu_zhou@cacch.com
 */
public record MeetingCreateScanResult(
        int scannedRows,
        int createdCount,
        int skippedCount,
        int failedCount
) {

    public static MeetingCreateScanResult empty() {
        return new MeetingCreateScanResult(0, 0, 0, 0);
    }

    public MeetingCreateScanResult merge(MeetingCreateScanResult other) {
        return new MeetingCreateScanResult(
                scannedRows + other.scannedRows,
                createdCount + other.createdCount,
                skippedCount + other.skippedCount,
                failedCount + other.failedCount);
    }
}
