package com.cacch.integration.integration.tencentmeeting.adapter;

import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingQueryResponse;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingRecordsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TencentMeetingRecordsAdapterTest {

    @Test
    void resolveMeetingId_matchByMeetingCode() {
        TencentMeetingQueryResponse response = new TencentMeetingQueryResponse();
        TencentMeetingQueryResponse.MeetingInfo info = new TencentMeetingQueryResponse.MeetingInfo();
        info.setMeetingId("7567173273889276131");
        info.setMeetingCode("806146667");
        response.setMeetingInfoList(List.of(info));

        assertEquals("7567173273889276131",
                TencentMeetingRecordsAdapter.resolveMeetingId(response, "806146667"));
    }

    @Test
    void resolveMeetingId_fallbackFirst() {
        TencentMeetingQueryResponse response = new TencentMeetingQueryResponse();
        TencentMeetingQueryResponse.MeetingInfo info = new TencentMeetingQueryResponse.MeetingInfo();
        info.setMeetingId("7567173273889276131");
        response.setMeetingInfoList(List.of(info));

        assertEquals("7567173273889276131",
                TencentMeetingRecordsAdapter.resolveMeetingId(response, null));
    }

    @Test
    void flattenRecordFiles() {
        TencentMeetingRecordsResponse response = new TencentMeetingRecordsResponse();
        TencentMeetingRecordsResponse.RecordMeeting recordMeeting = new TencentMeetingRecordsResponse.RecordMeeting();
        TencentMeetingRecordsResponse.RecordFile file = new TencentMeetingRecordsResponse.RecordFile();
        file.setRecordFileId("1318080818611111111");
        recordMeeting.setRecordFiles(List.of(file));
        response.setRecordMeetings(List.of(recordMeeting));

        assertEquals(1, TencentMeetingRecordsAdapter.flattenRecordFiles(response).size());
    }

    @Test
    void normalizeMeetingCode() {
        assertEquals("806146667", TencentMeetingRecordsAdapter.normalizeMeetingCode("806 146 667"));
        assertEquals("806146667", TencentMeetingRecordsAdapter.normalizeMeetingCode("806-146-667"));
    }

    @Test
    void isTranscoding() {
        assertTrue(TencentMeetingRecordsAdapter.isTranscoding(2));
        assertFalse(TencentMeetingRecordsAdapter.isTranscoding(3));
        assertNull(TencentMeetingRecordsAdapter.resolveMeetingId(null, "806146667"));
    }
}
