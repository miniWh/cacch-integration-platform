package com.cacch.integration.integration.tencentmeeting.adapter;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingRecordAddressesResponse;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 腾讯会议 record_file_id 解析适配器
 *
 * @author hongfu_zhou@cacch.com
 */
public final class TencentMeetingRecordFileResolver {

    private TencentMeetingRecordFileResolver() {
    }

    /**
     * 从腾讯会议录制列表中解析可用于 smart/minutes 的 record_file_id
     *
     * @param addresses        腾讯会议录制地址响应
     * @param wecomRecordFileId 企微 record/list 返回的 record_file_id
     * @param sessionIndex     场次序号（从 1 开始）
     * @return 腾讯会议 record_file_id，无法解析时返回 null
     */
    public static String resolveTencentRecordFileId(TencentMeetingRecordAddressesResponse addresses,
                                                    String wecomRecordFileId, int sessionIndex) {
        if (addresses == null || addresses.getRecordFiles() == null || addresses.getRecordFiles().isEmpty()) {
            return null;
        }
        List<TencentMeetingRecordAddressesResponse.RecordFile> recordFiles = addresses.getRecordFiles();
        if (StringUtils.hasText(wecomRecordFileId)) {
            for (TencentMeetingRecordAddressesResponse.RecordFile recordFile : recordFiles) {
                if (wecomRecordFileId.equals(recordFile.getRecordFileId())) {
                    return recordFile.getRecordFileId();
                }
            }
        }
        if (sessionIndex >= 1 && sessionIndex <= recordFiles.size()) {
            return recordFiles.get(sessionIndex - 1).getRecordFileId();
        }
        return recordFiles.getFirst().getRecordFileId();
    }
}
