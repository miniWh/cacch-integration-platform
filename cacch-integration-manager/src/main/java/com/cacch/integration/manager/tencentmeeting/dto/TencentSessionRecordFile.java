package com.cacch.integration.manager.tencentmeeting.dto;

/**
 * 腾讯会议单场录制文件
 *
 * @param recordFileId 腾讯会议 record_file_id
 * @param startTimeMs  录制开始时间（毫秒）
 * @param transcoding  是否转码中
 * @param recordState  录制状态：1=录制中，2=转码中，3=转码完成
 * @author hongfu_zhou@cacch.com
 */
public record TencentSessionRecordFile(String recordFileId, long startTimeMs, boolean transcoding,
                                       Integer recordState) {
}
