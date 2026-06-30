package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 企业微信会议 HTTP 客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComMeetingClient {

    private final RestTemplate restTemplate;

    public WeComCreateMeetingResponse createMeeting(String accessToken, WeComCreateMeetingRequest request) {
        String url = String.format(WeComConstants.MEETING_CREATE_URL, accessToken);
        log.info("【WeComMeeting】创建预约会议, title={}, admin={}", request.getTitle(), request.getAdminUserid());
        return post(url, request, WeComCreateMeetingResponse.class, "创建预约会议");
    }

    public WeComGetMeetingInfoResponse getMeetingInfo(String accessToken, WeComGetMeetingInfoRequest request) {
        String url = String.format(WeComConstants.MEETING_GET_INFO_URL, accessToken);
        log.info("【WeComMeeting】获取会议详情, meetingId={}", request.getMeetingid());
        return post(url, request, WeComGetMeetingInfoResponse.class, "获取会议详情");
    }

    public WeComGetTranscriptResponse getTranscriptDetail(String accessToken, WeComGetTranscriptRequest request) {
        String url = String.format(WeComConstants.MEETING_TRANSCRIPT_GET_DETAIL_URL, accessToken);
        log.info("【WeComMeeting】获取录制转写, meetingId={}", request.getMeetingid());
        return post(url, request, WeComGetTranscriptResponse.class, "获取录制转写");
    }

    private <T> T post(String url, Object request, Class<T> responseType, String action) {
        try {
            T response = restTemplate.postForObject(url, request, responseType);
            if (response == null) {
                log.error("【WeComMeeting】{} 返回 null", action);
                throw new RestClientException("企业微信" + action + "返回 null");
            }
            return response;
        } catch (RestClientException e) {
            log.error("【WeComMeeting】{} HTTP 调用失败", action, e);
            throw e;
        }
    }
}
