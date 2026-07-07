package com.cacch.integration.service.wecom.api.impl;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.WeComMeetingClient;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetRecordFileRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetRecordFileResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComListRecordRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComListRecordResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComRecordMeetingInfo;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComBaseResponse;
import com.cacch.integration.service.wecom.api.IWeComMeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 企微会议服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComMeetingServiceImpl implements IWeComMeetingService {

    private final WeComMeetingClient weComMeetingClient;

    @Override
    public WeComCreateMeetingResponse createMeeting(String accessToken, String adminUserid, String title,
                                                      long meetingStartEpochSec, int durationMinutes,
                                                      List<String> attendeeUserIds, String description,
                                                      String location) {
        int durationSec = Math.max(durationMinutes * 60, WeComConstants.MEETING_MIN_DURATION_SECONDS);
        WeComCreateMeetingRequest.WeComMeetingInvitees invitees = null;
        if (attendeeUserIds != null && !attendeeUserIds.isEmpty()) {
            invitees = WeComCreateMeetingRequest.WeComMeetingInvitees.builder()
                    .userid(attendeeUserIds)
                    .build();
        }
        WeComCreateMeetingRequest request = WeComCreateMeetingRequest.builder()
                .adminUserid(adminUserid)
                .title(title)
                .meetingStart(meetingStartEpochSec)
                .meetingDuration(durationSec)
                .description(description)
                .location(location)
                .invitees(invitees)
                .build();
        WeComCreateMeetingResponse response = weComMeetingClient.createMeeting(accessToken, request);
        assertWeComSuccess(response, "创建预约会议");
        return response;
    }

    @Override
    public WeComGetMeetingInfoResponse getMeetingInfo(String accessToken, String meetingId) {
        WeComGetMeetingInfoRequest request = WeComGetMeetingInfoRequest.builder()
                .meetingid(meetingId)
                .build();
        WeComGetMeetingInfoResponse response = weComMeetingClient.getMeetingInfo(accessToken, request);
        assertWeComSuccess(response, "获取会议详情");
        return response;
    }

    @Override
    public WeComGetTranscriptResponse getTranscriptDetail(String accessToken, String meetingId, String recordFileId) {
        WeComGetTranscriptRequest request = WeComGetTranscriptRequest.builder()
                .meetingid(meetingId)
                .recordFileId(recordFileId)
                .build();
        WeComGetTranscriptResponse response = weComMeetingClient.getTranscriptDetail(accessToken, request);
        assertWeComSuccess(response, "获取录制转写");
        return response;
    }

    @Override
    public WeComListRecordResponse listRecords(String accessToken, String meetingId,
                                               long startTimeSec, long endTimeSec) {
        List<WeComRecordMeetingInfo> allRecords = new ArrayList<>();
        String cursor = null;
        Integer limit = 20;
        boolean hasMore;
        do {
            WeComListRecordRequest request = WeComListRecordRequest.builder()
                    .meetingid(meetingId)
                    .startTime(startTimeSec)
                    .endTime(endTimeSec)
                    .cursor(cursor)
                    .limit(limit)
                    .build();
            WeComListRecordResponse response = weComMeetingClient.listRecords(accessToken, request);
            assertWeComSuccess(response, "获取录制列表");
            if (response.getRecordList() != null) {
                allRecords.addAll(response.getRecordList());
            }
            hasMore = Boolean.TRUE.equals(response.getHasMore());
            cursor = response.getNextCursor();
        } while (hasMore && cursor != null && !cursor.isBlank());
        WeComListRecordResponse merged = new WeComListRecordResponse();
        merged.setErrCode(0);
        merged.setErrMsg("ok");
        merged.setRecordList(allRecords);
        merged.setHasMore(false);
        return merged;
    }

    @Override
    public WeComGetRecordFileResponse getRecordFile(String accessToken, String meetingId, String recordFileId) {
        WeComGetRecordFileRequest request = WeComGetRecordFileRequest.builder()
                .meetingid(meetingId)
                .recordFileId(recordFileId)
                .build();
        WeComGetRecordFileResponse response = weComMeetingClient.getRecordFile(accessToken, request);
        assertWeComSuccess(response, "获取录制文件详情");
        return response;
    }

    @Override
    public String downloadText(String downloadUrl) {
        return weComMeetingClient.downloadText(downloadUrl);
    }

    @Override
    public byte[] downloadBytes(String downloadUrl) {
        return weComMeetingClient.downloadBytes(downloadUrl);
    }

    private void assertWeComSuccess(WeComBaseResponse response, String action) {
        if (!response.isSuccess()) {
            log.error("【WeComMeeting】{}失败, errcode={}, errmsg={}",
                    action, response.getErrCode(), response.getErrMsg());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    String.format("企业微信%s失败, errcode=%d, errmsg=%s",
                            action, response.getErrCode(), response.getErrMsg()));
        }
    }
}
