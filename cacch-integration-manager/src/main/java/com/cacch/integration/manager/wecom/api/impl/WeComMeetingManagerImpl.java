package com.cacch.integration.manager.wecom.api.impl;

import com.cacch.integration.common.config.wecom.WeComAppConfig;
import com.cacch.integration.common.config.wecom.WeComProperties;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptResponse;
import com.cacch.integration.manager.wecom.api.IWeComMeetingManager;
import com.cacch.integration.manager.wecom.api.IWeComTokenManager;
import com.cacch.integration.service.wecom.api.IWeComMeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeComMeetingManagerImpl implements IWeComMeetingManager {

    private final IWeComMeetingService weComMeetingService;
    private final IWeComTokenManager weComTokenManager;
    private final WeComProperties weComProperties;

    @Override
    public WeComCreateMeetingResponse createMeeting(String adminUserid, String title,
                                                      long meetingStartEpochSec, int durationMinutes,
                                                      List<String> attendeeUserIds) {
        return execute("创建预约会议", () ->
                weComMeetingService.createMeeting(resolveAccessToken(), adminUserid, title,
                        meetingStartEpochSec, durationMinutes, attendeeUserIds));
    }

    @Override
    public WeComGetMeetingInfoResponse getMeetingInfo(String meetingId) {
        return execute("获取会议详情", () ->
                weComMeetingService.getMeetingInfo(resolveAccessToken(), meetingId));
    }

    @Override
    public WeComGetTranscriptResponse getTranscriptDetail(String meetingId, String recordFileId) {
        return execute("获取录制转写", () ->
                weComMeetingService.getTranscriptDetail(resolveAccessToken(), meetingId, recordFileId));
    }

    private String resolveAccessToken() {
        WeComAppConfig appConfig = weComProperties.findSelfBuiltApp()
                .orElseThrow(() -> new BizException(ResultCode.PARAM_INVALID, "未配置企微自建应用（wecom.apps）"));
        return weComTokenManager.getAccessToken(appConfig.getCorpid(), appConfig.getAppKey());
    }

    private <T> T execute(String action, MeetingCall<T> call) {
        try {
            return call.run();
        } catch (BizException e) {
            log.error("【WeComMeeting】编排层{}失败, errCode={}, errMsg={}", action, e.getCode(), e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.error("【WeComMeeting】编排层{} HTTP 异常", action, e);
            throw new BizException(ResultCode.INTEGRATION_TIMEOUT, "企业微信" + action + "超时", e);
        } catch (Exception e) {
            log.error("【WeComMeeting】编排层{}发生未知异常", action, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "企业微信" + action + "失败", e);
        }
    }

    @FunctionalInterface
    private interface MeetingCall<T> {
        T run();
    }
}
