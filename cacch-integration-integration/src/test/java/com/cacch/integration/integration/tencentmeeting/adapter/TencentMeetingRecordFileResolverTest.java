package com.cacch.integration.integration.tencentmeeting.adapter;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingRecordAddressesResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TencentMeetingRecordFileResolverTest {

    @Test
    void resolveTencentRecordFileId_exactMatch() {
        TencentMeetingRecordAddressesResponse response = new TencentMeetingRecordAddressesResponse();
        TencentMeetingRecordAddressesResponse.RecordFile file1 = new TencentMeetingRecordAddressesResponse.RecordFile();
        file1.setRecordFileId("174530000000001");
        TencentMeetingRecordAddressesResponse.RecordFile file2 = new TencentMeetingRecordAddressesResponse.RecordFile();
        file2.setRecordFileId("174530000000002");
        response.setRecordFiles(List.of(file1, file2));

        assertEquals("174530000000002",
                TencentMeetingRecordFileResolver.resolveTencentRecordFileId(response, "174530000000002", 1));
    }

    @Test
    void resolveTencentRecordFileId_fallbackBySessionIndex() {
        TencentMeetingRecordAddressesResponse response = new TencentMeetingRecordAddressesResponse();
        TencentMeetingRecordAddressesResponse.RecordFile file1 = new TencentMeetingRecordAddressesResponse.RecordFile();
        file1.setRecordFileId("174530000000001");
        response.setRecordFiles(List.of(file1));

        assertEquals("174530000000001",
                TencentMeetingRecordFileResolver.resolveTencentRecordFileId(response, "wecom-token-id", 1));
    }

    @Test
    void resolveTencentRecordFileId_nullWhenEmpty() {
        assertNull(TencentMeetingRecordFileResolver.resolveTencentRecordFileId(null, "id", 1));
        assertNull(TencentMeetingRecordFileResolver.resolveTencentRecordFileId(new TencentMeetingRecordAddressesResponse(),
                "id", 1));
    }
}
