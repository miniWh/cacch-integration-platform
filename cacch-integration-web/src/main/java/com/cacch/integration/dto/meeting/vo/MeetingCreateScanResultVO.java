package com.cacch.integration.dto.meeting.vo;

import lombok.Data;

/**
 * 扫描建会结果 VO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MeetingCreateScanResultVO {

    /** 扫描到的表格行数 */
    private int scannedRows;

    /** 成功创建会议数 */
    private int createdCount;

    /** 跳过数（不满足建会条件或已是已创建） */
    private int skippedCount;

    /** 建会失败数 */
    private int failedCount;
}
