package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.common.constant.meeting.MeetingConstants;
import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.common.enums.meeting.MeetingMinutesStatusEnum;
import com.cacch.integration.common.enums.meeting.MeetingRecordStatusEnum;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.entity.meeting.SmartTableDO;
import com.cacch.integration.integration.wecom.adapter.MeetingSummaryDocumentExtractor;
import com.cacch.integration.integration.wecom.adapter.MeetingSummaryTodoParser;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetRecordFileResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComListRecordResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComMeetingSummaryFileInfo;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComRecordFileInfo;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComRecordMeetingInfo;
import com.cacch.integration.manager.meeting.api.IMeetingMinutesManager;
import com.cacch.integration.manager.wecom.api.IWeComMeetingManager;
import com.cacch.integration.service.meeting.api.ISmartTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会议纪要拉取与待办解析编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingMinutesManagerImpl implements IMeetingMinutesManager {

    private static final int RECORD_QUERY_MAX_DAYS = 31;
    private static final long SECONDS_PER_DAY = 24 * 60 * 60L;

    private final ISmartTableService smartTableService;
    private final IWeComMeetingManager weComMeetingManager;
    private final MeetingMinutesTxSupport meetingMinutesTxSupport;

    @Value("${meeting.sync.minutes-end-buffer-minutes:5}")
    private int minutesEndBufferMinutes;

    @Value("${meeting.sync.minutes-max-wait-hours:48}")
    private int minutesMaxWaitHours;

    @Override
    public int trySyncMinutes(MeetingRecordDO record) {
        String skipReason = checkEligibility(record);
        if (skipReason != null) {
            logSkip(record, skipReason);
            return 0;
        }
        SmartTableDO table = smartTableService.getById(record.getSmartTableId());
        if (table == null) {
            logSkip(record, "智能表格配置不存在, smartTableId=" + record.getSmartTableId());
            return 0;
        }
        if (!Integer.valueOf(MeetingConstants.SMART_TABLE_STATUS_ENABLED).equals(table.getStatus())) {
            logSkip(record, "智能表格未启用, smartTableId=" + table.getId() + ", status=" + table.getStatus());
            return 0;
        }
        try {
            WeComGetMeetingInfoResponse info = weComMeetingManager.getMeetingInfo(record.getWecomMeetingId());
            if (!isMeetingEnded(info, record)) {
                Long endEpochSec = resolveMeetingEndEpochSec(info, record);
                if (endEpochSec == null) {
                    logSkip(record, "无法解析会议结束时间，暂不拉取纪要");
                } else {
                    long readyAt = endEpochSec + minutesEndBufferMinutes * 60L;
                    logSkip(record, String.format(
                            "会议尚未结束或未过缓冲期, endEpochSec=%d, bufferMinutes=%d, readyAtEpochSec=%d, nowEpochSec=%d",
                            endEpochSec, minutesEndBufferMinutes, readyAt, Instant.now().getEpochSecond()));
                }
                return 0;
            }
            List<SessionRecordFile> sessionFiles = listSessionFiles(info, record);
            if (sessionFiles.isEmpty()) {
                if (shouldStopWaiting(info, record)) {
                    meetingMinutesTxSupport.finalizeWithoutTodos(record, table, "录制未就绪且已超过等待窗口");
                    log.info("【MeetingMinutes】等待窗口结束，标记纪要已生成（无待办）, recordId={}, meetingId={}, maxWaitHours={}",
                            record.getRecordId(), record.getWecomMeetingId(), minutesMaxWaitHours);
                    return 1;
                }
                logSkip(record, String.format(
                        "企微录制列表为空，继续等待, maxWaitHours=%d", minutesMaxWaitHours));
                return 0;
            }
            List<SessionRecordFile> transcodingFiles = sessionFiles.stream()
                    .filter(SessionRecordFile::transcoding)
                    .toList();
            if (!transcodingFiles.isEmpty()) {
                logSkip(record, String.format(
                        "存在转码中的录制, transcodingCount=%d, recordFileIds=%s",
                        transcodingFiles.size(),
                        transcodingFiles.stream().map(SessionRecordFile::recordFileId).collect(Collectors.joining(","))));
                return 0;
            }
            List<SessionRecordFile> readyFiles = assignSessionIndexes(sessionFiles);
            int sessionCount = readyFiles.size();
            log.info("【MeetingMinutes】开始解析纪要, recordId={}, meetingId={}, sessionCount={}",
                    record.getRecordId(), record.getWecomMeetingId(), sessionCount);
            List<String> allTodos = new ArrayList<>();
            StringBuilder rawContent = new StringBuilder();
            for (SessionRecordFile sessionFile : readyFiles) {
                SummaryParseResult parseResult = fetchTodosFromSummaryFiles(
                        record, sessionFile, sessionCount);
                if (StringUtils.hasText(parseResult.content())) {
                    if (!rawContent.isEmpty()) {
                        rawContent.append("\n\n");
                    }
                    rawContent.append(parseResult.content());
                }
                allTodos.addAll(parseResult.todos());
            }
            if (allTodos.isEmpty()) {
                log.info("【MeetingMinutes】全部场次均未解析到待办, recordId={}, meetingId={}, sessionCount={}",
                        record.getRecordId(), record.getWecomMeetingId(), sessionCount);
            }
            int createdCount = meetingMinutesTxSupport.persistMinutesAndTodos(
                    record, table, rawContent.toString(), allTodos);
            log.info("【MeetingMinutes】纪要待办已入库, recordId={}, meetingId={}, todoCount={}, created={}",
                    record.getRecordId(), record.getWecomMeetingId(), allTodos.size(), createdCount);
            return 1;
        } catch (Exception e) {
            log.error("【MeetingMinutes】纪要拉取失败, recordId={}, meetingId={}",
                    record.getRecordId(), record.getWecomMeetingId(), e);
            return -1;
        }
    }

    /**
     * @return null 表示可继续处理；非 null 为跳过原因
     */
    private String checkEligibility(MeetingRecordDO record) {
        if (record == null) {
            return "会议记录为空";
        }
        if (!MeetingRecordStatusEnum.SCHEDULED.getCode().equals(record.getStatus())) {
            return "会议状态非已创建(SCHEDULED), status=" + record.getStatus();
        }
        if (!StringUtils.hasText(record.getWecomMeetingId())) {
            return "缺少企微会议ID";
        }
        if (!StringUtils.hasText(record.getRecordId())) {
            return "缺少智能表格行 recordId";
        }
        String minutesStatus = record.getMinutesStatus();
        if (StringUtils.hasText(minutesStatus)
                && !MeetingMinutesStatusEnum.NONE.getCode().equals(minutesStatus)) {
            return "纪要状态非空/无, minutesStatus=" + minutesStatus;
        }
        return null;
    }

    private boolean isMeetingEnded(WeComGetMeetingInfoResponse info, MeetingRecordDO record) {
        Long endEpochSec = resolveMeetingEndEpochSec(info, record);
        if (endEpochSec == null) {
            return false;
        }
        long bufferSec = minutesEndBufferMinutes * 60L;
        return Instant.now().getEpochSecond() >= endEpochSec + bufferSec;
    }

    private boolean shouldStopWaiting(WeComGetMeetingInfoResponse info, MeetingRecordDO record) {
        Long endEpochSec = resolveMeetingEndEpochSec(info, record);
        if (endEpochSec == null) {
            return false;
        }
        long maxWaitSec = minutesMaxWaitHours * 3600L;
        return Instant.now().getEpochSecond() >= endEpochSec + maxWaitSec;
    }

    private Long resolveMeetingEndEpochSec(WeComGetMeetingInfoResponse info, MeetingRecordDO record) {
        if (info.getMeetingEnd() != null && info.getMeetingEnd() > 0) {
            return info.getMeetingEnd();
        }
        if (info.getMeetingStart() != null && info.getMeetingDuration() != null && info.getMeetingDuration() > 0) {
            return info.getMeetingStart() + info.getMeetingDuration();
        }
        if (record.getMeetingDate() != null && record.getStartTime() != null && record.getDuration() != null) {
            LocalDateTime start = LocalDateTime.of(record.getMeetingDate(), record.getStartTime());
            return start.plusMinutes(record.getDuration()).atZone(ZoneId.systemDefault()).toEpochSecond();
        }
        return null;
    }

    private List<SessionRecordFile> listSessionFiles(WeComGetMeetingInfoResponse info, MeetingRecordDO record) {
        long endSec = Instant.now().getEpochSecond();
        Long meetingStartSec = info.getMeetingStart();
        long startSec = meetingStartSec != null && meetingStartSec > 0
                ? meetingStartSec - SECONDS_PER_DAY
                : resolveLocalStartEpochSec(record) - SECONDS_PER_DAY;
        if (startSec < 0) {
            startSec = 0;
        }
        if (endSec - startSec > RECORD_QUERY_MAX_DAYS * SECONDS_PER_DAY) {
            startSec = endSec - RECORD_QUERY_MAX_DAYS * SECONDS_PER_DAY;
        }
        WeComListRecordResponse response = weComMeetingManager.listRecords(
                record.getWecomMeetingId(), startSec, endSec);
        if (response.getRecordList() == null || response.getRecordList().isEmpty()) {
            log.info("【MeetingMinutes】录制列表查询无结果, recordId={}, meetingId={}, startEpochSec={}, endEpochSec={}",
                    record.getRecordId(), record.getWecomMeetingId(), startSec, endSec);
            return List.of();
        }
        List<SessionRecordFile> files = new ArrayList<>();
        for (WeComRecordMeetingInfo meetingRecord : response.getRecordList()) {
            if (meetingRecord.getRecordFileList() == null || meetingRecord.getRecordFileList().isEmpty()) {
                log.info("【MeetingMinutes】录制项无文件列表, recordId={}, meetingId={}, meetingRecordId={}, state={}",
                        record.getRecordId(), record.getWecomMeetingId(),
                        meetingRecord.getMeetingRecordId(), meetingRecord.getState());
                continue;
            }
            boolean transcoding = meetingRecord.getState() != null
                    && meetingRecord.getState() != WeComConstants.MEETING_RECORD_STATE_TRANSCODED;
            for (WeComRecordFileInfo fileInfo : meetingRecord.getRecordFileList()) {
                if (!StringUtils.hasText(fileInfo.getRecordFileId())) {
                    log.info("【MeetingMinutes】跳过无 recordFileId 的录制文件, recordId={}, meetingId={}, state={}",
                            record.getRecordId(), record.getWecomMeetingId(), meetingRecord.getState());
                    continue;
                }
                long startTime = fileInfo.getRecordStartTime() != null ? fileInfo.getRecordStartTime() : 0L;
                files.add(new SessionRecordFile(fileInfo.getRecordFileId(), startTime, transcoding,
                        meetingRecord.getState(), 0));
            }
        }
        return files;
    }

    private List<SessionRecordFile> assignSessionIndexes(List<SessionRecordFile> files) {
        List<SessionRecordFile> sorted = files.stream()
                .sorted(Comparator.comparingLong(SessionRecordFile::startTime))
                .toList();
        List<SessionRecordFile> indexed = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            SessionRecordFile file = sorted.get(i);
            indexed.add(new SessionRecordFile(
                    file.recordFileId(), file.startTime(), file.transcoding(), file.recordState(), i + 1));
        }
        return indexed;
    }

    private long resolveLocalStartEpochSec(MeetingRecordDO record) {
        if (record.getMeetingDate() == null || record.getStartTime() == null) {
            return Instant.now().getEpochSecond();
        }
        return LocalDateTime.of(record.getMeetingDate(), record.getStartTime())
                .atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private SummaryParseResult fetchTodosFromSummaryFiles(MeetingRecordDO record, SessionRecordFile sessionFile,
                                                        int sessionCount) {
        String meetingId = record.getWecomMeetingId();
        WeComGetRecordFileResponse fileResponse = weComMeetingManager.getRecordFile(
                meetingId, sessionFile.recordFileId());
        List<WeComMeetingSummaryFileInfo> summaryFiles = fileResponse.resolveMeetingSummaryFiles();
        List<WeComMeetingSummaryFileInfo> transcriptFiles = fileResponse.resolveAiMeetingTranscriptFiles();
        log.info("【MeetingMinutes】录制文件详情, recordId={}, meetingId={}, sessionIndex={}, recordFileId={}, "
                        + "summaryFiles={}, transcriptFiles={}",
                record.getRecordId(), meetingId, sessionFile.sessionIndex(), sessionFile.recordFileId(),
                describeSummaryFiles(summaryFiles), describeSummaryFiles(transcriptFiles));
        if (summaryFiles.isEmpty()) {
            log.info("【MeetingMinutes】跳过，无 meeting_summary 文件, recordId={}, meetingId={}, sessionIndex={}, "
                            + "recordFileId={}",
                    record.getRecordId(), meetingId, sessionFile.sessionIndex(), sessionFile.recordFileId());
            return SummaryParseResult.empty();
        }
        List<WeComMeetingSummaryFileInfo> candidates = orderSummaryCandidates(summaryFiles);
        for (WeComMeetingSummaryFileInfo summaryFile : candidates) {
            if (!isSupportedSummaryType(summaryFile)) {
                log.info("【MeetingMinutes】跳过不支持的纪要文件类型, recordId={}, meetingId={}, fileType={}",
                        record.getRecordId(), meetingId, summaryFile.getFileType());
                continue;
            }
            String content = downloadAndExtractSummary(record, meetingId, sessionFile.recordFileId(), summaryFile);
            if (!StringUtils.hasText(content)) {
                log.info("【MeetingMinutes】纪要文件内容为空, recordId={}, meetingId={}, fileType={}",
                        record.getRecordId(), meetingId, summaryFile.getFileType());
                continue;
            }
            if (WeComConstants.MEETING_SUMMARY_FILE_TYPE_TXT.equalsIgnoreCase(summaryFile.getFileType())
                    && MeetingSummaryDocumentExtractor.isTranscriptLike(content)) {
                log.info("【MeetingMinutes】跳过 meeting_summary 中的转写 TXT（待办在 DOCX 纪要中）, recordId={}, "
                                + "meetingId={}, sessionIndex={}, recordFileId={}, contentLength={}",
                        record.getRecordId(), meetingId, sessionFile.sessionIndex(), sessionFile.recordFileId(),
                        content.length());
                continue;
            }
            List<String> todos = MeetingSummaryTodoParser.parseTodos(content);
            if (todos.isEmpty()) {
                log.info("【MeetingMinutes】纪要文件未解析出待办, recordId={}, meetingId={}, fileType={}, "
                                + "contentLength={}",
                        record.getRecordId(), meetingId, summaryFile.getFileType(), content.length());
                continue;
            }
            List<String> normalizedTodos = applySessionPrefix(todos, sessionFile.sessionIndex(), sessionCount);
            log.info("【MeetingMinutes】场次待办解析完成, recordId={}, meetingId={}, sessionIndex={}, fileType={}, "
                            + "todoCount={}",
                    record.getRecordId(), meetingId, sessionFile.sessionIndex(), summaryFile.getFileType(),
                    normalizedTodos.size());
            return new SummaryParseResult(content, normalizedTodos);
        }
        log.info("【MeetingMinutes】全部纪要文件均未解析到待办, recordId={}, meetingId={}, sessionIndex={}, recordFileId={}",
                record.getRecordId(), meetingId, sessionFile.sessionIndex(), sessionFile.recordFileId());
        return SummaryParseResult.empty();
    }

    private List<WeComMeetingSummaryFileInfo> orderSummaryCandidates(List<WeComMeetingSummaryFileInfo> summaryFiles) {
        List<WeComMeetingSummaryFileInfo> ordered = new ArrayList<>();
        summaryFiles.stream()
                .filter(file -> WeComConstants.MEETING_SUMMARY_FILE_TYPE_DOCX.equalsIgnoreCase(file.getFileType()))
                .forEach(ordered::add);
        summaryFiles.stream()
                .filter(file -> WeComConstants.MEETING_SUMMARY_FILE_TYPE_TXT.equalsIgnoreCase(file.getFileType()))
                .forEach(ordered::add);
        return ordered;
    }

    private boolean isSupportedSummaryType(WeComMeetingSummaryFileInfo fileInfo) {
        if (fileInfo == null || !StringUtils.hasText(fileInfo.getFileType())) {
            return false;
        }
        String fileType = fileInfo.getFileType().trim();
        return WeComConstants.MEETING_SUMMARY_FILE_TYPE_DOCX.equalsIgnoreCase(fileType)
                || WeComConstants.MEETING_SUMMARY_FILE_TYPE_TXT.equalsIgnoreCase(fileType);
    }

    private String describeSummaryFiles(List<WeComMeetingSummaryFileInfo> files) {
        if (files == null || files.isEmpty()) {
            return "none";
        }
        return files.stream()
                .map(file -> file.getFileType() != null ? file.getFileType() : "unknown")
                .collect(Collectors.joining(","));
    }

    private List<String> applySessionPrefix(List<String> todos, int sessionIndex, int sessionCount) {
        if (sessionCount <= 1) {
            return todos;
        }
        List<String> prefixed = new ArrayList<>(todos.size());
        String prefix = "第" + sessionIndex + "场：";
        for (String todo : todos) {
            if (!StringUtils.hasText(todo)) {
                continue;
            }
            prefixed.add(todo.startsWith(prefix) ? todo : prefix + todo);
        }
        return prefixed;
    }

    private String downloadAndExtractSummary(MeetingRecordDO record, String meetingId, String recordFileId,
                                             WeComMeetingSummaryFileInfo summaryFile) {
        byte[] bytes = downloadSummaryBytes(record, meetingId, recordFileId, summaryFile);
        return MeetingSummaryDocumentExtractor.extractText(summaryFile.getFileType(), bytes);
    }

    /**
     * 下载纪要文件；403 时重新调用 get_file 获取新签名 URL 后重试一次
     */
    private byte[] downloadSummaryBytes(MeetingRecordDO record, String meetingId, String recordFileId,
                                        WeComMeetingSummaryFileInfo summaryFile) {
        try {
            return weComMeetingManager.downloadBytes(summaryFile.getDownloadAddress());
        } catch (RestClientException e) {
            if (!(e instanceof HttpClientErrorException.Forbidden)) {
                throw e;
            }
            log.info("【MeetingMinutes】纪要下载403，重新获取下载地址后重试, recordId={}, meetingId={}, recordFileId={}, "
                            + "fileType={}",
                    record.getRecordId(), meetingId, recordFileId, summaryFile.getFileType());
            WeComGetRecordFileResponse refreshed = weComMeetingManager.getRecordFile(meetingId, recordFileId);
            WeComMeetingSummaryFileInfo refreshedFile = findSameTypeSummaryFile(
                    refreshed.resolveMeetingSummaryFiles(), summaryFile.getFileType());
            if (refreshedFile == null || !StringUtils.hasText(refreshedFile.getDownloadAddress())) {
                log.info("【MeetingMinutes】重试失败，刷新后无可用纪要下载地址, recordId={}, meetingId={}, "
                                + "recordFileId={}, fileType={}",
                        record.getRecordId(), meetingId, recordFileId, summaryFile.getFileType());
                throw e;
            }
            return weComMeetingManager.downloadBytes(refreshedFile.getDownloadAddress());
        }
    }

    private WeComMeetingSummaryFileInfo findSameTypeSummaryFile(List<WeComMeetingSummaryFileInfo> files,
                                                                String fileType) {
        if (files == null || !StringUtils.hasText(fileType)) {
            return null;
        }
        return files.stream()
                .filter(file -> fileType.equalsIgnoreCase(file.getFileType()))
                .findFirst()
                .orElse(null);
    }

    private void logSkip(MeetingRecordDO record, String reason) {
        if (record == null) {
            log.info("【MeetingMinutes】跳过纪要拉取, reason={}", reason);
            return;
        }
        log.info("【MeetingMinutes】跳过纪要拉取, recordId={}, meetingId={}, reason={}",
                record.getRecordId(), record.getWecomMeetingId(), reason);
    }

    private record SessionRecordFile(String recordFileId, long startTime, boolean transcoding,
                                     Integer recordState, int sessionIndex) {
    }

    private record SummaryParseResult(String content, List<String> todos) {

        private static SummaryParseResult empty() {
            return new SummaryParseResult(null, List.of());
        }
    }
}
