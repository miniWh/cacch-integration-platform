package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.config.tencentmeeting.TencentMeetingProperties;
import com.cacch.integration.common.constant.meeting.MeetingConstants;
import com.cacch.integration.common.enums.meeting.MeetingMinutesStatusEnum;
import com.cacch.integration.common.enums.meeting.MeetingRecordStatusEnum;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.entity.meeting.SmartTableDO;
import com.cacch.integration.entity.meeting.TodoItemDO;
import com.cacch.integration.integration.tencentmeeting.adapter.TencentMeetingRecordsAdapter;
import com.cacch.integration.integration.tencentmeeting.adapter.TencentMeetingSmartMinutesAdapter;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;
import com.cacch.integration.integration.wecom.adapter.MeetingSummaryTodoParser;
import com.cacch.integration.manager.meeting.api.IMeetingMinutesManager;
import com.cacch.integration.manager.tencentmeeting.api.ITencentMeetingManager;
import com.cacch.integration.manager.tencentmeeting.dto.TencentSessionRecordFile;
import com.cacch.integration.service.meeting.api.ISmartTableService;
import com.cacch.integration.service.meeting.api.ITodoItemService;
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
    private final ITencentMeetingManager tencentMeetingManager;
    private final TencentMeetingProperties tencentMeetingProperties;
    private final MeetingMinutesTxSupport meetingMinutesTxSupport;
    private final TodoSheetWriteSupport todoSheetWriteSupport;
    private final ITodoItemService todoItemService;

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
        if (!tencentMeetingProperties.isEnabled()) {
            logSkip(record, "腾讯会议 API 未启用（tencent-meeting.enabled=false）");
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
            if (!isMeetingEnded(record)) {
                Long endEpochSec = resolveMeetingEndEpochSec(record);
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
            List<SessionRecordFile> sessionFiles = listSessionFiles(record);
            if (sessionFiles.isEmpty()) {
                if (shouldStopWaiting(record)) {
                    meetingMinutesTxSupport.markMinutesNotObtained(record, table, "录制未就绪且已超过等待窗口");
                    log.info("【MeetingMinutes】等待窗口结束，未获取到纪要, recordId={}, meetingCode={}, maxWaitHours={}",
                            record.getRecordId(), resolveMeetingCode(record), minutesMaxWaitHours);
                    return 1;
                }
                logSkip(record, String.format(
                        "腾讯录制列表为空，继续等待, maxWaitHours=%d", minutesMaxWaitHours));
                markMinutesPending(record, table, "腾讯录制列表为空");
                return 0;
            }
            List<SessionRecordFile> transcodingFiles = sessionFiles.stream()
                    .filter(SessionRecordFile::transcoding)
                    .toList();
            if (!transcodingFiles.isEmpty()) {
                if (shouldStopWaiting(record)) {
                    meetingMinutesTxSupport.markMinutesNotObtained(record, table, "录制转码超时");
                    log.info("【MeetingMinutes】等待窗口结束，录制仍在转码, recordId={}, meetingCode={}, transcodingCount={}",
                            record.getRecordId(), resolveMeetingCode(record), transcodingFiles.size());
                    return 1;
                }
                logSkip(record, String.format(
                        "存在转码中的录制, transcodingCount=%d, recordFileIds=%s",
                        transcodingFiles.size(),
                        transcodingFiles.stream().map(SessionRecordFile::recordFileId).collect(Collectors.joining(","))));
                markMinutesPending(record, table, "录制转码中");
                return 0;
            }
            List<SessionRecordFile> readyFiles = assignSessionIndexes(sessionFiles);
            int sessionCount = readyFiles.size();
            log.info("【MeetingMinutes】开始拉取腾讯会议智能纪要, recordId={}, meetingCode={}, txMeetingId={}, sessionCount={}",
                    record.getRecordId(), resolveMeetingCode(record), resolveTxMeetingId(record), sessionCount);
            List<String> allTodos = new ArrayList<>();
            StringBuilder rawContent = new StringBuilder();
            for (SessionRecordFile sessionFile : readyFiles) {
                SummaryParseResult parseResult = fetchTodosFromTencentSmartMinutes(
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
                log.info("【MeetingMinutes】全部场次均未解析到待办, recordId={}, meetingCode={}, sessionCount={}",
                        record.getRecordId(), resolveMeetingCode(record), sessionCount);
            }
            if (!StringUtils.hasText(rawContent.toString())) {
                if (shouldStopWaiting(record)) {
                    meetingMinutesTxSupport.markMinutesNotObtained(record, table, "智能纪要未生成且已超过等待窗口");
                    log.info("【MeetingMinutes】等待窗口结束，未获取到智能纪要, recordId={}, meetingCode={}",
                            record.getRecordId(), resolveMeetingCode(record));
                    return 1;
                }
                logSkip(record, String.format("智能纪要未就绪，继续等待, maxWaitHours=%d", minutesMaxWaitHours));
                markMinutesPending(record, table, "智能纪要未就绪");
                return 0;
            }
            int createdCount = meetingMinutesTxSupport.persistMinutesAndTodos(
                    record, table, rawContent.toString(), allTodos);
            syncPendingTodosToSheet(record, table);
            log.info("【MeetingMinutes】纪要待办已入库, recordId={}, meetingCode={}, todoCount={}, created={}",
                    record.getRecordId(), resolveMeetingCode(record), allTodos.size(), createdCount);
            return 1;
        } catch (Exception e) {
            log.info("【MeetingMinutes】单条纪要拉取终止, recordId={}, meetingCode={}, reason={}",
                    record.getRecordId(), resolveMeetingCode(record), e.getMessage());
            log.error("【MeetingMinutes】纪要拉取失败, recordId={}, meetingCode={}, txMeetingId={}",
                    record.getRecordId(), resolveMeetingCode(record), resolveTxMeetingId(record), e);
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
        if (!StringUtils.hasText(record.getRecordId())) {
            return "缺少智能表格行 recordId";
        }
        if (!hasTencentMeetingLookupKey(record)) {
            return "缺少会议号或腾讯 meeting_id";
        }
        String minutesStatus = record.getMinutesStatus();
        if (StringUtils.hasText(minutesStatus)
                && !MeetingMinutesStatusEnum.NONE.getCode().equals(minutesStatus)
                && !MeetingMinutesStatusEnum.PENDING.getCode().equals(minutesStatus)) {
            return "纪要状态非无/待解析, minutesStatus=" + minutesStatus;
        }
        return null;
    }

    private boolean hasTencentMeetingLookupKey(MeetingRecordDO record) {
        return StringUtils.hasText(record.getWecomMeetingCode())
                || TencentMeetingRecordsAdapter.isTencentMeetingId(record.getWecomMeetingId());
    }

    private boolean isMeetingEnded(MeetingRecordDO record) {
        Long endEpochSec = resolveMeetingEndEpochSec(record);
        if (endEpochSec == null) {
            return false;
        }
        long bufferSec = minutesEndBufferMinutes * 60L;
        return Instant.now().getEpochSecond() >= endEpochSec + bufferSec;
    }

    private boolean shouldStopWaiting(MeetingRecordDO record) {
        Long endEpochSec = resolveMeetingEndEpochSec(record);
        if (endEpochSec == null) {
            return false;
        }
        long maxWaitSec = minutesMaxWaitHours * 3600L;
        return Instant.now().getEpochSecond() >= endEpochSec + maxWaitSec;
    }

    private Long resolveMeetingEndEpochSec(MeetingRecordDO record) {
        if (record.getMeetingDate() != null && record.getStartTime() != null && record.getDuration() != null) {
            LocalDateTime start = LocalDateTime.of(record.getMeetingDate(), record.getStartTime());
            return start.plusMinutes(record.getDuration()).atZone(ZoneId.systemDefault()).toEpochSecond();
        }
        return null;
    }

    private List<SessionRecordFile> listSessionFiles(MeetingRecordDO record) {
        String meetingCode = resolveMeetingCode(record);
        String txMeetingId = resolveTxMeetingId(record);
        if (!hasTencentMeetingLookupKey(record)) {
            log.info("【MeetingMinutes】缺少会议号/腾讯 meeting_id，无法查询腾讯录制, recordId={}",
                    record.getRecordId());
            return List.of();
        }
        String wecomOperatorId = resolveWecomOperatorId(record);
        if (!StringUtils.hasText(wecomOperatorId)) {
            log.info("【MeetingMinutes】无法解析 operatorId，无法查询腾讯录制, recordId={}, meetingCode={}, txMeetingId={}",
                    record.getRecordId(), meetingCode, txMeetingId);
            return List.of();
        }

        long endSec = Instant.now().getEpochSecond();
        long startSec = resolveLocalStartEpochSec(record) - SECONDS_PER_DAY;
        if (startSec < 0) {
            startSec = 0;
        }
        if (endSec - startSec > RECORD_QUERY_MAX_DAYS * SECONDS_PER_DAY) {
            startSec = endSec - RECORD_QUERY_MAX_DAYS * SECONDS_PER_DAY;
        }

        List<TencentSessionRecordFile> txFiles;
        try {
            txFiles = tencentMeetingManager.listSessionRecordFiles(
                    meetingCode, txMeetingId, startSec, endSec, wecomOperatorId);
        } catch (BizException e) {
            if (e.getMessage() != null && e.getMessage().contains("未找到企微用户对应的腾讯会议 userid")) {
                log.info("【MeetingMinutes】跳过，企微用户未映射腾讯会议 userid, recordId={}, meetingCode={}, txMeetingId={}, "
                                + "wecomOperatorId={}, reason={}",
                        record.getRecordId(), meetingCode, txMeetingId, wecomOperatorId, e.getMessage());
                return List.of();
            }
            throw e;
        }
        if (txFiles.isEmpty()) {
            log.info("【MeetingMinutes】腾讯录制列表查询无结果, recordId={}, meetingCode={}, txMeetingId={}, "
                            + "startEpochSec={}, endEpochSec={}",
                    record.getRecordId(), meetingCode, txMeetingId, startSec, endSec);
            return List.of();
        }
        List<SessionRecordFile> files = new ArrayList<>(txFiles.size());
        for (TencentSessionRecordFile txFile : txFiles) {
            files.add(new SessionRecordFile(txFile.recordFileId(), txFile.startTimeMs(),
                    txFile.transcoding(), txFile.recordState(), 0));
        }
        return files;
    }

    private List<SessionRecordFile> assignSessionIndexes(List<SessionRecordFile> files) {
        List<SessionRecordFile> sorted = files.stream()
                .sorted(Comparator.comparingLong(SessionRecordFile::startTimeMs))
                .toList();
        List<SessionRecordFile> indexed = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            SessionRecordFile file = sorted.get(i);
            indexed.add(new SessionRecordFile(
                    file.recordFileId(), file.startTimeMs(), file.transcoding(),
                    file.recordState(), i + 1));
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

    private SummaryParseResult fetchTodosFromTencentSmartMinutes(MeetingRecordDO record,
                                                                 SessionRecordFile sessionFile,
                                                                 int sessionCount) {
        String wecomOperatorId = resolveWecomOperatorId(record);
        if (!StringUtils.hasText(wecomOperatorId)) {
            log.info("【MeetingMinutes】跳过，无法解析企微 operatorId, recordId={}, sessionIndex={}, recordFileId={}",
                    record.getRecordId(), sessionFile.sessionIndex(), sessionFile.recordFileId());
            return SummaryParseResult.empty();
        }
        log.info("【MeetingMinutes】调用腾讯会议智能纪要, meetingCode={}, meetingTitle={}, txMeetingId={}, recordId={}, "
                        + "sessionIndex={}, txRecordFileId={}, wecomOperatorId={}",
                resolveMeetingCode(record), resolveMeetingTitle(record), resolveTxMeetingId(record),
                record.getRecordId(), sessionFile.sessionIndex(),
                sessionFile.recordFileId(), wecomOperatorId);

        TencentMeetingSmartMinutesResponse response;
        try {
            response = tencentMeetingManager.getSmartMinutes(sessionFile.recordFileId(), wecomOperatorId);
        } catch (BizException e) {
            if (e.getMessage() != null && e.getMessage().contains("未找到企微用户对应的腾讯会议 userid")) {
                log.info("【MeetingMinutes】跳过，企微用户未映射腾讯会议 userid, recordId={}, sessionIndex={}, "
                                + "recordFileId={}, wecomOperatorId={}, reason={}",
                        record.getRecordId(), sessionFile.sessionIndex(), sessionFile.recordFileId(),
                        wecomOperatorId, e.getMessage());
                return SummaryParseResult.empty();
            }
            throw e;
        }
        if (response == null) {
            log.info("【MeetingMinutes】智能纪要未就绪, recordId={}, sessionIndex={}, recordFileId={}",
                    record.getRecordId(), sessionFile.sessionIndex(), sessionFile.recordFileId());
            return SummaryParseResult.empty();
        }
        String minuteText = TencentMeetingSmartMinutesAdapter.resolveMinuteText(response);
        String todoSource = TencentMeetingSmartMinutesAdapter.resolveTodoSourceText(response);
        if (!StringUtils.hasText(todoSource)) {
            log.info("【MeetingMinutes】智能纪要无待办文本, recordId={}, sessionIndex={}, recordFileId={}, "
                            + "minutePreview={}",
                    record.getRecordId(), sessionFile.sessionIndex(), sessionFile.recordFileId(),
                    previewContent(minuteText));
            return new SummaryParseResult(minuteText, List.of());
        }
        List<String> todos = MeetingSummaryTodoParser.parseTodos(todoSource);
        if (todos.isEmpty()) {
            log.info("【MeetingMinutes】智能纪要未解析出待办, recordId={}, sessionIndex={}, recordFileId={}, "
                            + "todoSourcePreview={}",
                    record.getRecordId(), sessionFile.sessionIndex(), sessionFile.recordFileId(),
                    previewContent(todoSource));
            return new SummaryParseResult(minuteText, List.of());
        }
        List<String> normalizedTodos = applySessionPrefix(todos, sessionFile.sessionIndex(), sessionCount);
        log.info("【MeetingMinutes】场次待办解析完成, recordId={}, sessionIndex={}, source=tencent-smart-minutes, "
                        + "todoCount={}",
                record.getRecordId(), sessionFile.sessionIndex(), normalizedTodos.size());
        return new SummaryParseResult(minuteText, normalizedTodos);
    }

    private void syncPendingTodosToSheet(MeetingRecordDO record, SmartTableDO table) {
        List<TodoItemDO> pendingTodos = todoItemService.listByMeetingId(record.getId()).stream()
                .filter(todo -> !StringUtils.hasText(todo.getRecordId()))
                .toList();
        if (pendingTodos.isEmpty()) {
            return;
        }
        int syncedCount = todoSheetWriteSupport.writeTodosToSheet(table, pendingTodos);
        log.info("【MeetingMinutes】待办回写子表, recordId={}, meetingCode={}, pending={}, synced={}",
                record.getRecordId(), resolveMeetingCode(record), pendingTodos.size(), syncedCount);
    }

    private String resolveWecomOperatorId(MeetingRecordDO record) {
        if (record.getAttendees() != null) {
            for (String attendee : record.getAttendees()) {
                if (StringUtils.hasText(attendee)) {
                    return attendee.trim();
                }
            }
        }
        if (StringUtils.hasText(tencentMeetingProperties.getDefaultOperatorId())) {
            return tencentMeetingProperties.getDefaultOperatorId().trim();
        }
        return null;
    }

    private String previewContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        int maxLen = 300;
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "...";
    }

    private String resolveMeetingCode(MeetingRecordDO record) {
        if (record != null && StringUtils.hasText(record.getWecomMeetingCode())) {
            return TencentMeetingRecordsAdapter.normalizeMeetingCode(record.getWecomMeetingCode());
        }
        return null;
    }

    private String resolveTxMeetingId(MeetingRecordDO record) {
        if (record != null && TencentMeetingRecordsAdapter.isTencentMeetingId(record.getWecomMeetingId())) {
            return record.getWecomMeetingId().trim();
        }
        return null;
    }

    private String resolveMeetingTitle(MeetingRecordDO record) {
        if (record != null && StringUtils.hasText(record.getMeetingTitle())) {
            return record.getMeetingTitle();
        }
        return "-";
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

    private void markMinutesPending(MeetingRecordDO record, SmartTableDO table, String reason) {
        meetingMinutesTxSupport.markMinutesPending(record, table, reason);
    }

    private void logSkip(MeetingRecordDO record, String reason) {
        if (record == null) {
            log.info("【MeetingMinutes】跳过纪要拉取, reason={}", reason);
            return;
        }
        log.info("【MeetingMinutes】跳过纪要拉取, recordId={}, meetingCode={}, txMeetingId={}, reason={}",
                record.getRecordId(), resolveMeetingCode(record), resolveTxMeetingId(record), reason);
    }

    private record SessionRecordFile(String recordFileId, long startTimeMs, boolean transcoding,
                                     Integer recordState, int sessionIndex) {
    }

    private record SummaryParseResult(String content, List<String> todos) {

        private static SummaryParseResult empty() {
            return new SummaryParseResult(null, List.of());
        }
    }
}
