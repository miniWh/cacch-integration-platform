package com.cacch.integration.manager.tencentmeeting.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.tencentmeeting.adapter.TencentMeetingRecordsAdapter;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingQueryResponse;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingRecordsResponse;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;
import com.cacch.integration.manager.tencentmeeting.api.ITencentMeetingManager;
import com.cacch.integration.manager.tencentmeeting.dto.TencentSessionRecordFile;
import com.cacch.integration.service.tencentmeeting.api.ITencentMeetingService;
import com.cacch.integration.service.tencentmeeting.api.ITencentMeetingUserMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯会议编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TencentMeetingManagerImpl implements ITencentMeetingManager {

    private final ITencentMeetingService tencentMeetingService;
    private final ITencentMeetingUserMappingService tencentMeetingUserMappingService;

    @Override
    public List<TencentSessionRecordFile> listSessionRecordFiles(String meetingCode, String txMeetingId,
                                                                 long startTimeSec, long endTimeSec,
                                                                 String wecomOperatorId) {
        String normalizedMeetingCode = TencentMeetingRecordsAdapter.normalizeMeetingCode(meetingCode);
        String normalizedTxMeetingId = StringUtils.hasText(txMeetingId) ? txMeetingId.trim() : null;
        if (!StringUtils.hasText(normalizedMeetingCode)
                && !TencentMeetingRecordsAdapter.isTencentMeetingId(normalizedTxMeetingId)) {
            log.info("【TencentMeeting】会议号和 meeting_id 均为空，无法查询录制列表");
            return List.of();
        }
        String txOperatorId = resolveTxMeetingUserId(wecomOperatorId);

        String meetingId = resolveTencentMeetingId(normalizedMeetingCode, normalizedTxMeetingId, txOperatorId);
        if (!StringUtils.hasText(meetingId)) {
            log.info("【TencentMeeting】未解析到 meeting_id, meetingCode={}, txMeetingId={}",
                    normalizedMeetingCode, normalizedTxMeetingId);
            return List.of();
        }
        log.info("【TencentMeeting】查询录制列表, meetingCode={}, meetingId={}",
                normalizedMeetingCode, meetingId);

        TencentMeetingRecordsResponse recordsResponse = tencentMeetingService.listRecords(
                meetingId, normalizedMeetingCode, startTimeSec, endTimeSec, txOperatorId);
        return toSessionRecordFiles(recordsResponse);
    }

    private String resolveTencentMeetingId(String meetingCode, String txMeetingId, String txOperatorId) {
        if (TencentMeetingRecordsAdapter.isTencentMeetingId(txMeetingId)) {
            log.info("【TencentMeeting】使用已有 meeting_id, meetingId={}", txMeetingId);
            return txMeetingId;
        }
        if (!StringUtils.hasText(meetingCode)) {
            return null;
        }
        TencentMeetingQueryResponse meetingResponse =
                tencentMeetingService.getMeetingByCode(meetingCode, txOperatorId);
        String meetingId = TencentMeetingRecordsAdapter.resolveMeetingId(meetingResponse, meetingCode);
        if (StringUtils.hasText(meetingId)) {
            log.info("【TencentMeeting】会议号映射 meeting_id, meetingCode={}, meetingId={}",
                    meetingCode, meetingId);
        }
        return meetingId;
    }

    @Override
    public TencentMeetingSmartMinutesResponse getSmartMinutes(String recordFileId, String wecomOperatorId) {
        String txOperatorId = resolveTxMeetingUserId(wecomOperatorId);
        return tencentMeetingService.getSmartMinutes(recordFileId, txOperatorId);
    }

    @Override
    public String resolveTxMeetingUserId(String wecomUserId) {
        if (!StringUtils.hasText(wecomUserId)) {
            throw new BizException(ResultCode.PARAM_INVALID, "企微 userid 为空，无法调用腾讯会议 API");
        }
        String normalizedWecomUserId = wecomUserId.trim();
        String txMeetingUserId = tencentMeetingUserMappingService.resolveTxMeetingUserId(normalizedWecomUserId);
        if (!StringUtils.hasText(txMeetingUserId)) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "未找到企微用户对应的腾讯会议 userid, wecomUserId=" + normalizedWecomUserId);
        }
        log.info("【TencentMeeting】用户ID映射, wecomUserId={}, txMeetingUserId={}",
                normalizedWecomUserId, txMeetingUserId);
        return txMeetingUserId;
    }

    private List<TencentSessionRecordFile> toSessionRecordFiles(TencentMeetingRecordsResponse recordsResponse) {
        if (recordsResponse == null || recordsResponse.getRecordMeetings() == null) {
            return List.of();
        }
        List<TencentSessionRecordFile> sessionFiles = new ArrayList<>();
        for (TencentMeetingRecordsResponse.RecordMeeting recordMeeting : recordsResponse.getRecordMeetings()) {
            Integer state = recordMeeting.getState();
            boolean transcoding = TencentMeetingRecordsAdapter.isTranscoding(state);
            if (recordMeeting.getRecordFiles() == null || recordMeeting.getRecordFiles().isEmpty()) {
                log.info("【TencentMeeting】录制项无文件列表, meetingRecordId={}, state={}",
                        recordMeeting.getMeetingRecordId(), state);
                continue;
            }
            for (TencentMeetingRecordsResponse.RecordFile recordFile : recordMeeting.getRecordFiles()) {
                if (!StringUtils.hasText(recordFile.getRecordFileId())) {
                    continue;
                }
                long startTimeMs = recordFile.getRecordStartTime() != null ? recordFile.getRecordStartTime() : 0L;
                sessionFiles.add(new TencentSessionRecordFile(
                        recordFile.getRecordFileId(), startTimeMs, transcoding, state));
            }
        }
        log.info("【TencentMeeting】录制文件解析完成, fileCount={}", sessionFiles.size());
        return sessionFiles;
    }
}
