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
        List<String> inviteeUserIds = orderInviteesWithCreatorFirst(adminUserid, attendeeUserIds);
        WeComCreateMeetingRequest.WeComMeetingInvitees invitees = null;
        if (!inviteeUserIds.isEmpty()) {
            invitees = WeComCreateMeetingRequest.WeComMeetingInvitees.builder()
                    .userid(inviteeUserIds)
                    .build();
        }
        // 将参会人第一人同时设为管理员与主持人，确保企微侧会议创建人/管理身份正确
        WeComCreateMeetingRequest.WeComMeetingSettings settings = WeComCreateMeetingRequest.WeComMeetingSettings.builder()
                .hosts(WeComCreateMeetingRequest.WeComMeetingHosts.builder()
                        .userid(List.of(adminUserid))
                        .build())
                .build();
        WeComCreateMeetingRequest request = WeComCreateMeetingRequest.builder()
                .adminUserid(adminUserid)
                .title(title)
                .meetingStart(meetingStartEpochSec)
                .meetingDuration(durationSec)
                .description(description)
                .location(location)
                .invitees(invitees)
                .settings(settings)
                .build();
        log.info("【WeComMeeting】组装建会请求, admin={}, invitees={}, hosts={}",
                adminUserid, inviteeUserIds, adminUserid);
        WeComCreateMeetingResponse response = weComMeetingClient.createMeeting(accessToken, request);
        assertWeComSuccess(response, "创建预约会议");
        return response;
    }

    /**
     * 邀请人列表保序去重，并将会议创建人（admin）置于首位。
     */
    private List<String> orderInviteesWithCreatorFirst(String adminUserid, List<String> attendeeUserIds) {
        List<String> ordered = new ArrayList<>();
        if (adminUserid != null && !adminUserid.isBlank()) {
            ordered.add(adminUserid.trim());
        }
        if (attendeeUserIds == null) {
            return ordered;
        }
        for (String userId : attendeeUserIds) {
            if (userId == null || userId.isBlank()) {
                continue;
            }
            String normalized = userId.trim();
            if (!ordered.contains(normalized)) {
                ordered.add(normalized);
            }
        }
        return ordered;
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
            log.info("【WeComMeeting】{}终止, errcode={}, reason={}",
                    action, response.getErrCode(), response.getErrMsg());
            log.error("【WeComMeeting】{}失败, errcode={}, errmsg={}",
                    action, response.getErrCode(), response.getErrMsg());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    String.format("企业微信%s失败, errcode=%d, errmsg=%s",
                            action, response.getErrCode(), response.getErrMsg()));
        }
    }
}
