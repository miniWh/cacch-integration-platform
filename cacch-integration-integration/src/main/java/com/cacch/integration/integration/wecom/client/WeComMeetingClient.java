package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetRecordFileRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetRecordFileResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComListRecordRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComListRecordResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptRequest;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetTranscriptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * 企业微信会议 HTTP 客户端
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComMeetingClient {

    private static final String BIZ = "WeComMeeting";

    private final RestTemplate restTemplate;

    /**
     * 创建企微预约会议
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     创建会议请求体，不可为空
     * @return 创建会议响应（含 meetingid、会议号、链接）
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComCreateMeetingResponse createMeeting(String accessToken, WeComCreateMeetingRequest request) {
        String url = String.format(WeComConstants.MEETING_CREATE_URL, accessToken);
        return post(url, request, WeComCreateMeetingResponse.class, "创建预约会议");
    }

    /**
     * 获取企微会议详情
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     查询请求（含 meetingid），不可为空
     * @return 会议详情响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComGetMeetingInfoResponse getMeetingInfo(String accessToken, WeComGetMeetingInfoRequest request) {
        String url = String.format(WeComConstants.MEETING_GET_INFO_URL, accessToken);
        return post(url, request, WeComGetMeetingInfoResponse.class, "获取会议详情");
    }

    /**
     * 获取会议录制转写详情
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     转写查询请求，不可为空
     * @return 转写详情响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComGetTranscriptResponse getTranscriptDetail(String accessToken, WeComGetTranscriptRequest request) {
        String url = String.format(WeComConstants.MEETING_TRANSCRIPT_GET_DETAIL_URL, accessToken);
        return post(url, request, WeComGetTranscriptResponse.class, "获取录制转写");
    }

    /**
     * 获取会议录制列表
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     录制列表查询请求，不可为空
     * @return 录制列表响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComListRecordResponse listRecords(String accessToken, WeComListRecordRequest request) {
        String url = String.format(WeComConstants.MEETING_RECORD_LIST_URL, accessToken);
        return post(url, request, WeComListRecordResponse.class, "获取录制列表");
    }

    /**
     * 获取单个录制文件详情
     *
     * @param accessToken 企微 access_token，禁止写入日志
     * @param request     录制文件查询请求，不可为空
     * @return 录制文件详情响应
     * @throws RestClientException 网络错误或接口返回 null
     */
    public WeComGetRecordFileResponse getRecordFile(String accessToken, WeComGetRecordFileRequest request) {
        String url = String.format(WeComConstants.MEETING_RECORD_GET_FILE_URL, accessToken);
        return post(url, request, WeComGetRecordFileResponse.class, "获取录制文件详情");
    }

    /**
     * 下载二进制文件（用于 DOCX 纪要）
     *
     * @param downloadUrl 下载地址，不可为空
     * @return 文件字节；响应体为空时返回空数组
     * @throws RestClientException 地址非法、403 或网络错误
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
                log.error("【WeComMeeting】下载纪要文件失败, url={}",
                        ThirdPartyHttpLogSupport.maskUrl(normalizedUrl), e);
            }
            throw e;
        }
    }

    /**
     * 下载文本文件内容（用于会议纪要 TXT）
     *
     * <p>腾讯云 COS 预签名 URL 对 query 参数编码敏感，必须使用已编码 URI 发起请求，
     * 否则 RestTemplate 二次编码会导致签名校验失败（403 AccessDenied / Request has expired）。</p>
     *
     * @param downloadUrl 下载地址，不可为空
     * @return 文件文本内容（UTF-8）
     * @throws RestClientException 地址非法、403 或网络错误
     */
    public String downloadText(String downloadUrl) {
        byte[] bytes = downloadBytes(downloadUrl);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private <T> T post(String url, Object request, Class<T> responseType, String action) {
        ThirdPartyHttpLogSupport.logRequest(BIZ, action, url, request);
        try {
            T response = restTemplate.postForObject(url, request, responseType);
            ThirdPartyHttpLogSupport.logResponse(BIZ, action, response);
            if (response == null) {
                log.info("【WeComMeeting】{}终止, reason=接口返回null", action);
                log.error("【WeComMeeting】{} 返回 null", action);
                throw new RestClientException("企业微信" + action + "返回 null");
            }
            return response;
        } catch (RestClientException e) {
            log.info("【WeComMeeting】{}终止, reason={}", action, e.getMessage());
            log.error("【WeComMeeting】{} HTTP 调用失败", action, e);
            throw e;
        }
    }
}
