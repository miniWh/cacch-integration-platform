package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetRecordFileRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetRecordFileResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComListRecordRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComListRecordResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * 企业微信会议 HTTP 客户端
 *
 * <p>HTTP 通用调用统一由 {@link RestTemplateHttpClient} 承担；
 * 企微鉴权特殊：access_token 直接内嵌在 URL query 参数中。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComMeetingClient {

    private static final String BIZ = "WeComMeeting";

    private final RestTemplateHttpClient httpClient;
    private final RestTemplate restTemplate;

    public WeComCreateMeetingResponse createMeeting(String accessToken, WeComCreateMeetingRequest request) {
        String url = String.format(WeComConstants.MEETING_CREATE_URL, accessToken);
        return post(url, request, WeComCreateMeetingResponse.class, "创建预约会议");
    }

    public WeComGetMeetingInfoResponse getMeetingInfo(String accessToken, WeComGetMeetingInfoRequest request) {
        String url = String.format(WeComConstants.MEETING_GET_INFO_URL, accessToken);
        return post(url, request, WeComGetMeetingInfoResponse.class, "获取会议详情");
    }

    public WeComGetTranscriptResponse getTranscriptDetail(String accessToken, WeComGetTranscriptRequest request) {
        String url = String.format(WeComConstants.MEETING_TRANSCRIPT_GET_DETAIL_URL, accessToken);
        return post(url, request, WeComGetTranscriptResponse.class, "获取录制转写");
    }

    public WeComListRecordResponse listRecords(String accessToken, WeComListRecordRequest request) {
        String url = String.format(WeComConstants.MEETING_RECORD_LIST_URL, accessToken);
        return post(url, request, WeComListRecordResponse.class, "获取录制列表");
    }

    public WeComGetRecordFileResponse getRecordFile(String accessToken, WeComGetRecordFileRequest request) {
        String url = String.format(WeComConstants.MEETING_RECORD_GET_FILE_URL, accessToken);
        return post(url, request, WeComGetRecordFileResponse.class, "获取录制文件详情");
    }

    private <T> T post(String url, Object request, Class<T> responseType, String action) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return httpClient.doPostOrPut(HttpMethod.POST, url, request, headers, BIZ, action, responseType);
    }

    /**
     * 下载二进制文件（用于 DOCX 纪要）
     *
     * <p>腾讯云 COS 预签名 URL 对 query 参数编码敏感，必须使用已编码 URI 发起请求，
     * 否则 RestTemplate 二次编码会导致签名校验失败。</p>
     */
    public byte[] downloadBytes(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new RestClientException("企业微信纪要下载地址为空");
        }
        String normalizedUrl = downloadUrl.trim();
        String action = "下载纪要文件";
        ThirdPartyHttpLogSupport.logRequest(BIZ, action, normalizedUrl, Map.of("method", "GET"));
        try {
            URI uri = UriComponentsBuilder.fromUriString(normalizedUrl).build(true).toUri();
            RequestEntity<Void> request = RequestEntity.get(uri)
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; CacchIntegration/1.0)")
                    .build();
            ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);
            byte[] body = response.getBody() != null ? response.getBody() : new byte[0];
            ThirdPartyHttpLogSupport.logResponse(BIZ, action,
                    Map.of("statusCode", response.getStatusCode().value(), "byteLength", body.length));
            return body;
        } catch (IllegalArgumentException e) {
            log.info("【WeComMeeting】纪要下载终止, reason=下载地址非法");
            log.error("【WeComMeeting】纪要下载地址非法, url={}", ThirdPartyHttpLogSupport.maskUrl(normalizedUrl), e);
            throw new RestClientException("企业微信纪要下载地址非法", e);
        } catch (RestClientException e) {
            if (e instanceof HttpClientErrorException.Forbidden) {
                log.info("【WeComMeeting】纪要下载终止, reason=403签名URL可能已过期");
                log.error("【WeComMeeting】下载纪要文件403（签名URL可能已过期或被二次编码破坏）, url={}",
                        ThirdPartyHttpLogSupport.maskUrl(normalizedUrl), e);
            } else {
                log.info("【WeComMeeting】纪要下载终止, reason={}", e.getMessage());
                log.error("【WeComMeeting】下载纪要文件失败, url={}", ThirdPartyHttpLogSupport.maskUrl(normalizedUrl), e);
            }
            throw e;
        }
    }

    /**
     * 下载文本文件内容（用于会议纪要 TXT）
     */
    public String downloadText(String downloadUrl) {
        byte[] bytes = downloadBytes(downloadUrl);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
