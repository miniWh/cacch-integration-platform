package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.common.constant.meeting.MeetingConstants;
import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.common.enums.meeting.MeetingMinutesStatusEnum;
import com.cacch.integration.common.enums.meeting.MeetingRecordStatusEnum;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.entity.meeting.SmartTableDO;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        if (!isEligible(record)) {
            return 0;
        }
        SmartTableDO table = smartTableService.getById(record.getSmartTableId());
        if (table == null
                || !Integer.valueOf(MeetingConstants.SMART_TABLE_STATUS_ENABLED).equals(table.getStatus())) {
            return 0;
        }
        try {
            WeComGetMeetingInfoResponse info = weComMeetingManager.getMeetingInfo(record.getWecomMeetingId());
            if (!isMeetingEnded(info, record)) {
                return 0;
            }
            List<SessionRecordFile> sessionFiles = listSessionFiles(info, record);
            if (sessionFiles.isEmpty()) {
                if (shouldStopWaiting(info, record)) {
                    meetingMinutesTxSupport.finalizeWithoutTodos(record, table);
                    log.info("【MeetingMinutes】录制未就绪且已超过等待窗口, recordId={}, meetingId={}",
                            record.getRecordId(), record.getWecomMeetingId());
                    return 1;
                }
                log.info("【MeetingMinutes】录制未就绪, recordId={}, meetingId={}",
                        record.getRecordId(), record.getWecomMeetingId());
                return 0;
            }
            if (sessionFiles.stream().anyMatch(SessionRecordFile::transcoding)) {
                log.info("【MeetingMinutes】存在转码中的录制, recordId={}, meetingId={}",
                        record.getRecordId(), record.getWecomMeetingId());
                return 0;
            }
            List<SessionRecordFile> readyFiles = assignSessionIndexes(sessionFiles);
            int sessionCount = readyFiles.size();
            List<String> allTodos = new ArrayList<>();
            StringBuilder rawContent = new StringBuilder();
            for (SessionRecordFile sessionFile : readyFiles) {
                List<String> sessionTodos = fetchTodosFromTxtSummary(
                        record.getWecomMeetingId(), sessionFile, sessionCount, rawContent);
                allTodos.addAll(sessionTodos);
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

    private boolean isEligible(MeetingRecordDO record) {
        if (record == null || !MeetingRecordStatusEnum.SCHEDULED.getCode().equals(record.getStatus())) {
            return false;
        }
        if (!StringUtils.hasText(record.getWecomMeetingId()) || !StringUtils.hasText(record.getRecordId())) {
            return false;
        }
        String minutesStatus = record.getMinutesStatus();
        return !StringUtils.hasText(minutesStatus)
                || MeetingMinutesStatusEnum.NONE.getCode().equals(minutesStatus);
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
            return List.of();
        }
        List<SessionRecordFile> files = new ArrayList<>();
        for (WeComRecordMeetingInfo meetingRecord : response.getRecordList()) {
            if (meetingRecord.getRecordFileList() == null) {
                continue;
            }
            boolean transcoding = meetingRecord.getState() != null
                    && meetingRecord.getState() != WeComConstants.MEETING_RECORD_STATE_TRANSCODED;
            for (WeComRecordFileInfo fileInfo : meetingRecord.getRecordFileList()) {
                if (!StringUtils.hasText(fileInfo.getRecordFileId())) {
                    continue;
                }
                long startTime = fileInfo.getRecordStartTime() != null ? fileInfo.getRecordStartTime() : 0L;
                files.add(new SessionRecordFile(fileInfo.getRecordFileId(), startTime, transcoding, 0));
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
                    file.recordFileId(), file.startTime(), file.transcoding(), i + 1));
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

    private List<String> fetchTodosFromTxtSummary(String meetingId, SessionRecordFile sessionFile,
                                                  int sessionCount, StringBuilder rawContent) {
        WeComGetRecordFileResponse fileResponse = weComMeetingManager.getRecordFile(
                meetingId, sessionFile.recordFileId());
        WeComMeetingSummaryFileInfo txtSummary = fileResponse.resolveMeetingSummaryFiles().stream()
                .filter(this::isTxtSummary)
                .findFirst()
                .orElse(null);
        if (txtSummary == null) {
            log.info("【MeetingMinutes】跳过非 TXT 或无纪要文件, meetingId={}, recordFileId={}",
                    meetingId, sessionFile.recordFileId());
            return List.of();
        }
        String content = weComMeetingManager.downloadText(txtSummary.getDownloadAddress());
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        if (!rawContent.isEmpty()) {
            rawContent.append("\n\n");
        }
        rawContent.append(content);
        List<String> todos = MeetingSummaryTodoParser.parseTodos(content);
        if (sessionCount <= 1) {
            return todos;
        }
        List<String> prefixed = new ArrayList<>(todos.size());
        String prefix = "第" + sessionFile.sessionIndex() + "场：";
        for (String todo : todos) {
            if (!StringUtils.hasText(todo)) {
                continue;
            }
            prefixed.add(todo.startsWith(prefix) ? todo : prefix + todo);
        }
        return prefixed;
    }

    private boolean isTxtSummary(WeComMeetingSummaryFileInfo fileInfo) {
        return fileInfo != null
                && StringUtils.hasText(fileInfo.getDownloadAddress())
                && StringUtils.hasText(fileInfo.getFileType())
                && WeComConstants.MEETING_SUMMARY_FILE_TYPE_TXT.equalsIgnoreCase(fileInfo.getFileType().trim());
    }

    private record SessionRecordFile(String recordFileId, long startTime, boolean transcoding, int sessionIndex) {
    }
}
