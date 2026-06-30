package com.cacch.integration.service.wecom.api;

import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptResponse;

import java.util.List;

/**
 * 企业微信会议服务接口
 */
public interface IWeComMeetingService {

    WeComCreateMeetingResponse createMeeting(String accessToken, String adminUserid, String title,
                                               long meetingStartEpochSec, int durationMinutes,
                                               List<String> attendeeUserIds);

    WeComGetMeetingInfoResponse getMeetingInfo(String accessToken, String meetingId);

    WeComGetTranscriptResponse getTranscriptDetail(String accessToken, String meetingId, String recordFileId);
}
