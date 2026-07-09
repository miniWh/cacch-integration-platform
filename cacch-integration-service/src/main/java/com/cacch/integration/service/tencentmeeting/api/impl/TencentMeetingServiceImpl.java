package com.cacch.integration.service.tencentmeeting.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.tencentmeeting.client.TencentMeetingClient;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingQueryResponse;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingRecordsResponse;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;
import com.cacch.integration.service.tencentmeeting.api.ITencentMeetingService;
import com.tencentcloudapi.wemeet.core.exception.ClientException;
import com.tencentcloudapi.wemeet.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 腾讯会议服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentMeetingServiceImpl implements ITencentMeetingService {

    private final TencentMeetingClient tencentMeetingClient;

    @Override
    public TencentMeetingQueryResponse getMeetingByCode(String meetingCode, String operatorId) {
        if (!tencentMeetingClient.isEnabled()) {
            throw new BizException(ResultCode.PARAM_INVALID, "腾讯会议 API 未启用");
        }
        try {
            return tencentMeetingClient.getMeetingByCode(meetingCode, operatorId);
        } catch (ServiceException e) {
            log.info("【TencentMeeting】会议号查询终止, meetingCode={}, operatorId={}, reason={}",
                    meetingCode, operatorId, summarizeError(e));
            log.error("【TencentMeeting】通过会议号查询失败, meetingCode={}, operatorId={}, detail={}",
                    meetingCode, operatorId, summarizeError(e));
            throw new BizException(ResultCode.INTEGRATION_ERROR, "腾讯会议查询失败: " + summarizeError(e), e);
        } catch (ClientException e) {
            log.info("【TencentMeeting】会议号查询终止, meetingCode={}, operatorId={}, reason={}",
                    meetingCode, operatorId, e.getMessage());
            log.error("【TencentMeeting】会议查询客户端异常, meetingCode={}, operatorId={}",
                    meetingCode, operatorId, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "腾讯会议查询调用失败", e);
        }
    }

    @Override
    public TencentMeetingRecordsResponse listRecords(String meetingId, String meetingCode,
                                                     long startTimeSec, long endTimeSec, String operatorId) {
        if (!tencentMeetingClient.isEnabled()) {
            throw new BizException(ResultCode.PARAM_INVALID, "腾讯会议 API 未启用");
        }
        try {
            return tencentMeetingClient.listRecords(meetingId, meetingCode, startTimeSec, endTimeSec, operatorId);
        } catch (ServiceException e) {
            log.info("【TencentMeeting】录制列表查询终止, meetingId={}, meetingCode={}, operatorId={}, reason={}",
                    meetingId, meetingCode, operatorId, summarizeError(e));
            log.error("【TencentMeeting】查询录制列表失败, meetingId={}, meetingCode={}, operatorId={}, detail={}",
                    meetingId, meetingCode, operatorId, summarizeError(e));
            throw new BizException(ResultCode.INTEGRATION_ERROR, "腾讯会议录制列表查询失败: " + summarizeError(e), e);
        } catch (ClientException e) {
            log.info("【TencentMeeting】录制列表查询终止, meetingId={}, meetingCode={}, operatorId={}, reason={}",
                    meetingId, meetingCode, operatorId, e.getMessage());
            log.error("【TencentMeeting】录制列表客户端异常, meetingId={}, meetingCode={}, operatorId={}",
                    meetingId, meetingCode, operatorId, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "腾讯会议录制列表调用失败", e);
        }
    }

    @Override
    public TencentMeetingSmartMinutesResponse getSmartMinutes(String recordFileId, String operatorId) {
        if (!tencentMeetingClient.isEnabled()) {
            throw new BizException(ResultCode.PARAM_INVALID, "腾讯会议 API 未启用");
        }
        try {
            return tencentMeetingClient.getSmartMinutes(recordFileId, operatorId);
        } catch (ServiceException e) {
            if (isMinutesNotReady(e)) {
                log.info("【TencentMeeting】智能纪要未就绪, recordFileId={}, reason={}",
                        recordFileId, e.getErrorInfo().getMessage());
                return null;
            }
            log.info("【TencentMeeting】智能纪要获取终止, recordFileId={}, operatorId={}, reason={}",
                    recordFileId, operatorId, summarizeError(e));
            log.error("【TencentMeeting】获取智能纪要失败, recordFileId={}, operatorId={}, detail={}",
                    recordFileId, operatorId, summarizeError(e));
            throw new BizException(ResultCode.INTEGRATION_ERROR, "腾讯会议智能纪要获取失败: " + summarizeError(e), e);
        } catch (ClientException e) {
            log.info("【TencentMeeting】智能纪要获取终止, recordFileId={}, operatorId={}, reason={}",
                    recordFileId, operatorId, e.getMessage());
            log.error("【TencentMeeting】智能纪要客户端异常, recordFileId={}, operatorId={}",
                    recordFileId, operatorId, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "腾讯会议智能纪要调用失败", e);
        }
    }

    private boolean isMinutesNotReady(ServiceException e) {
        String message = summarizeError(e);
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("生成中")
                || normalized.contains("generating")
                || normalized.contains("not ready")
                || normalized.contains("未开启")
                || normalized.contains("未生成");
    }

    /**
     * 汇总腾讯会议业务异常信息；禁止输出完整 raw body，避免敏感字段入日志。
     *
     * @param e 腾讯会议 ServiceException
     * @return 可读错误摘要（优先 errorInfo.message，其次截断后的 raw body）
     */
    private String summarizeError(ServiceException e) {
        if (e.getErrorInfo() != null && StringUtils.hasText(e.getErrorInfo().getMessage())) {
            return e.getErrorInfo().getMessage();
        }
        if (e.getApiResp() != null && e.getApiResp().getRawBody() != null) {
            String raw = new String(e.getApiResp().getRawBody());
            return truncateForLog(raw, 200);
        }
        return e.getMessage();
    }

    private String truncateForLog(String value, int maxLen) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen) + "...(truncated)";
    }
}
