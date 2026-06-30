package com.cacch.integration.manager.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptResponse;

import java.util.List;

/**
 * 企业微信会议编排接口
 */
public interface IWeComMeetingManager {

    WeComCreateMeetingResponse createMeeting(String adminUserid, String title,
                                                 long meetingStartEpochSec, int durationMinutes,
                                                 List<String> attendeeUserIds);

    WeComGetMeetingInfoResponse getMeetingInfo(String meetingId);

    WeComGetTranscriptResponse getTranscriptDetail(String meetingId, String recordFileId);
}
