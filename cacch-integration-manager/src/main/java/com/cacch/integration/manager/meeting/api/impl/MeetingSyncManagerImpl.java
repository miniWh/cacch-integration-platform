package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.common.constant.meeting.MeetingConstants;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.common.enums.meeting.MeetingMinutesStatusEnum;
import com.cacch.integration.common.enums.meeting.MeetingRecordStatusEnum;
import com.cacch.integration.common.enums.meeting.SmartTableTypeEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.entity.meeting.SmartTableDO;
import com.cacch.integration.entity.meeting.TodoItemDO;
import com.cacch.integration.integration.wecom.adapter.WeComSmartSheetCellAdapter;
import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComCreateMeetingResponse;
import com.cacch.integration.integration.wecom.client.dto.meeting.WeComGetMeetingInfoResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordInfo;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordWriteItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComSheetInfo;
import com.cacch.integration.manager.meeting.api.IMeetingSyncManager;
import com.cacch.integration.manager.wecom.api.IWeComDocManager;
import com.cacch.integration.manager.wecom.api.IWeComMeetingManager;
import com.cacch.integration.manager.wecom.api.IWeComSmartSheetManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import com.cacch.integration.service.meeting.api.IMeetingRecordService;
import com.cacch.integration.service.meeting.api.ISmartTableService;
import com.cacch.integration.service.meeting.api.ITodoItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会议智能表格同步编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingSyncManagerImpl implements IMeetingSyncManager {

    private static final String BIZ = "meeting";
    private static final int RECORD_PAGE_SIZE = 100;

    private final ISmartTableService smartTableService;
    private final IMeetingRecordService meetingRecordService;
    private final ITodoItemService todoItemService;
    private final IWeComSmartSheetManager weComSmartSheetManager;
    private final IWeComDocManager weComDocManager;
    private final IWeComMeetingManager weComMeetingManager;
    private final IWeComWebhookManager weComWebhookManager;

    @Override
    public void scanMasterAndProvision() {
        SmartTableDO master = smartTableService.getEnabledMaster();
        if (master == null) {
            log.warn("【MeetingSync】未配置启用的总控表(MASTER)，跳过扫描");
            return;
        }
        log.info("【MeetingSync】开始扫描总控表, docId={}", master.getDocId());
        try {
            WeComGetRecordsResponse recordsResponse = weComSmartSheetManager.getRecords(
                    master.getDocId(), master.getMeetingSheetId(), 0, RECORD_PAGE_SIZE);
            if (recordsResponse.getRecords() == null) {
                return;
            }
            Map<String, String> mapping = master.getMeetingColumnMapping();
            for (WeComRecordInfo record : recordsResponse.getRecords()) {
                provisionEmployeeTable(master, mapping, record);
            }
            smartTableService.markSyncSuccess(master.getId());
        } catch (Exception e) {
            smartTableService.markSyncError(master.getId(), e.getMessage());
            throw e;
        }
    }

    @Override
    public void syncMeetingRecordsFromSheets() {
        List<SmartTableDO> meetingTables = smartTableService.listEnabledMeetingTables();
        for (SmartTableDO table : meetingTables) {
            syncSingleMeetingTable(table);
        }
    }

    @Override
    public void createPendingWeComMeetings() {
        List<MeetingRecordDO> pendingRecords = meetingRecordService.listByStatus(
                MeetingRecordStatusEnum.PENDING.getCode());
        for (MeetingRecordDO record : pendingRecords) {
            createWeComMeetingForRecord(record);
        }
    }

    @Override
    public void syncTodosToSheet() {
        List<SmartTableDO> meetingTables = smartTableService.listEnabledMeetingTables();
        for (SmartTableDO table : meetingTables) {
            if (!StringUtils.hasText(table.getTodoSheetId()) || table.getTodoColumnMapping() == null) {
                continue;
            }
            List<TodoItemDO> todos = todoItemService.listPendingBySmartTableId(table.getId());
            for (TodoItemDO todo : todos) {
                if (StringUtils.hasText(todo.getRecordId())) {
                    continue;
                }
                writeTodoToSheet(table, todo);
            }
        }
    }

    private void provisionEmployeeTable(SmartTableDO master, Map<String, String> mapping, WeComRecordInfo record) {
        Map<String, Object> values = record.getValues();
        String applyStatus = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "apply_status");
        if (!MeetingConstants.APPLY_STATUS_APPROVED.equals(applyStatus)) {
            return;
        }
        String createdDocId = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "created_doc_id");
        if (StringUtils.hasText(createdDocId)) {
            return;
        }
        String userId = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "user_id");
        if (!StringUtils.hasText(userId)) {
            log.warn("【MeetingSync】总控行缺少 user_id, recordId={}", record.getRecordId());
            return;
        }
        String userName = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "user_name");
        String docName = StringUtils.hasText(userName) ? userName + "的会议管理" : userId + "的会议管理";

        SmartTableDO existingMeeting = smartTableService.getEnabledMeetingByUserId(userId);
        if (existingMeeting != null) {
            writeBackMasterProvision(master, mapping, record.getRecordId(), docName,
                    existingMeeting.getDocId(), existingMeeting.getDocUrl());
            log.info("【MeetingSync】总控回写重试（本地已有会议表）, userId={}, docId={}",
                    userId, existingMeeting.getDocId());
            return;
        }

        WeComCreateDocResponse docResponse = weComDocManager.createSmartSheetDoc(docName, List.of(userId));
        WeComGetSheetResponse sheetResponse = weComSmartSheetManager.getSheets(docResponse.getDocid(), null, false);
        String meetingSheetId = resolveFirstSheetId(sheetResponse);

        SmartTableDO meetingTable = new SmartTableDO();
        meetingTable.setTableType(SmartTableTypeEnum.MEETING.getCode());
        meetingTable.setUserId(userId);
        meetingTable.setTableName(docName);
        meetingTable.setDocId(docResponse.getDocid());
        meetingTable.setDocUrl(docResponse.getUrl());
        meetingTable.setMeetingSheetId(meetingSheetId);
        meetingTable.setMeetingColumnMapping(copyColumnMapping(master.getMeetingColumnMapping()));
        meetingTable.setStatus(MeetingConstants.SMART_TABLE_STATUS_ENABLED);
        smartTableService.saveNew(meetingTable);

        writeBackMasterProvision(master, mapping, record.getRecordId(), docName,
                docResponse.getDocid(), docResponse.getUrl());
        log.info("【MeetingSync】为员工创建会议管理表, userId={}, docId={}", userId, docResponse.getDocid());
    }

    private void syncSingleMeetingTable(SmartTableDO table) {
        log.info("【MeetingSync】同步会议行, smartTableId={}, docId={}", table.getId(), table.getDocId());
        try {
            WeComGetRecordsResponse response = weComSmartSheetManager.getRecords(
                    table.getDocId(), table.getMeetingSheetId(), 0, RECORD_PAGE_SIZE);
            if (response.getRecords() == null) {
                smartTableService.markSyncSuccess(table.getId());
                return;
            }
            Map<String, String> mapping = table.getMeetingColumnMapping();
            for (WeComRecordInfo row : response.getRecords()) {
                upsertMeetingRecord(table, mapping, row);
            }
            smartTableService.markSyncSuccess(table.getId());
        } catch (Exception e) {
            smartTableService.markSyncError(table.getId(), e.getMessage());
            weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                    .biz(BIZ)
                    .title("智能表格同步异常")
                    .subject(table.getTableName())
                    .context("smartTableId=" + table.getId() + ", docId=" + table.getDocId())
                    .error(e)
                    .dedupType("table")
                    .dedupId(String.valueOf(table.getId()))
                    .build());
            log.error("【MeetingSync】同步会议行失败, smartTableId={}", table.getId(), e);
        }
    }

    private void upsertMeetingRecord(SmartTableDO table, Map<String, String> mapping, WeComRecordInfo row) {
        Map<String, Object> values = row.getValues();
        String title = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "meeting_title");
        if (!StringUtils.hasText(title)) {
            return;
        }
        MeetingRecordDO existing = meetingRecordService.getBySmartTableIdAndRecordId(table.getId(), row.getRecordId());
        MeetingRecordDO record = existing != null ? existing : new MeetingRecordDO();
        record.setSmartTableId(table.getId());
        record.setRecordId(row.getRecordId());
        record.setMeetingTitle(title);
        record.setMeetingDate(parseDate(WeComSmartSheetCellAdapter.getMappedText(values, mapping, "meeting_date")));
        record.setStartTime(parseTime(WeComSmartSheetCellAdapter.getMappedText(values, mapping, "start_time")));
        record.setDuration(parseDuration(WeComSmartSheetCellAdapter.getMappedText(values, mapping, "duration")));
        record.setMeetingLink(WeComSmartSheetCellAdapter.getMappedText(values, mapping, "meeting_link"));
        String sheetStatus = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "status");
        if (StringUtils.hasText(sheetStatus) && existing == null) {
            record.setStatus(mapSheetStatus(sheetStatus));
        } else if (existing == null) {
            record.setStatus(MeetingRecordStatusEnum.PENDING.getCode());
            record.setMinutesStatus(MeetingMinutesStatusEnum.NONE.getCode());
        }
        if (existing == null) {
            meetingRecordService.save(record);
        } else {
            meetingRecordService.updateById(record);
        }
    }

    private void createWeComMeetingForRecord(MeetingRecordDO record) {
        SmartTableDO table = smartTableService.getById(record.getSmartTableId());
        if (table == null) {
            return;
        }
        if (record.getMeetingDate() == null || record.getStartTime() == null) {
            log.warn("【MeetingSync】会议缺少日期/时间, recordId={}", record.getId());
            return;
        }
        LocalDateTime startDateTime = LocalDateTime.of(record.getMeetingDate(), record.getStartTime());
        long epochSec = startDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        WeComCreateMeetingResponse meetingResponse = weComMeetingManager.createMeeting(
                table.getUserId(),
                record.getMeetingTitle(),
                epochSec,
                record.getDuration() != null ? record.getDuration() : 30,
                record.getAttendees());

        WeComGetMeetingInfoResponse info = weComMeetingManager.getMeetingInfo(meetingResponse.getMeetingid());
        record.setWecomMeetingId(meetingResponse.getMeetingid());
        record.setWecomMeetingCode(info.getMeetingCode());
        record.setStatus(MeetingRecordStatusEnum.SCHEDULED.getCode());
        meetingRecordService.updateById(record);

        writeBackMeetingStatus(table, record);
        log.info("【MeetingSync】创建企微会议成功, recordId={}, meetingId={}", record.getId(), meetingResponse.getMeetingid());
    }

    private void writeBackMasterProvision(SmartTableDO master, Map<String, String> mapping,
                                          String recordId, String linkText,
                                          String docId, String docUrl) {
        Map<String, Object> values = new HashMap<>();
        putTextValue(values, mapping, "created_doc_id", docId);
        putUrlValue(values, mapping, "created_doc_url", linkText, docUrl);
        WeComRecordWriteItem item = WeComRecordWriteItem.builder()
                .recordId(recordId)
                .values(values)
                .build();
        weComSmartSheetManager.updateRecords(master.getDocId(), master.getMeetingSheetId(), List.of(item));
    }

    private void writeBackMeetingStatus(SmartTableDO table, MeetingRecordDO record) {
        Map<String, String> mapping = table.getMeetingColumnMapping();
        if (mapping == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        putTextValue(values, mapping, "status", MeetingRecordStatusEnum.SCHEDULED.getDesc());
        if (StringUtils.hasText(record.getWecomMeetingCode())) {
            putTextValue(values, mapping, "meeting_link", record.getWecomMeetingCode());
        }
        WeComRecordWriteItem item = WeComRecordWriteItem.builder()
                .recordId(record.getRecordId())
                .values(values)
                .build();
        weComSmartSheetManager.updateRecords(table.getDocId(), table.getMeetingSheetId(), List.of(item));
    }

    private void writeTodoToSheet(SmartTableDO table, TodoItemDO todo) {
        Map<String, String> mapping = table.getTodoColumnMapping();
        Map<String, Object> values = new HashMap<>();
        putTextValue(values, mapping, "todo_title", todo.getTodoTitle());
        putTextValue(values, mapping, "assignee", todo.getAssigneeName());
        if (todo.getDueDate() != null) {
            putTextValue(values, mapping, "due_date", todo.getDueDate().toString());
        }
        putTextValue(values, mapping, "priority", todo.getPriority());
        putTextValue(values, mapping, "status", todo.getStatus());

        WeComRecordWriteItem item = WeComRecordWriteItem.builder().values(values).build();
        var response = weComSmartSheetManager.addRecords(table.getDocId(), table.getTodoSheetId(), List.of(item));
        if (response.getRecords() != null && !response.getRecords().isEmpty()) {
            todo.setRecordId(response.getRecords().getFirst().getRecordId());
            todoItemService.updateById(todo);
        }
    }

    private void putTextValue(Map<String, Object> values, Map<String, String> mapping,
                              String logicalKey, String text) {
        if (mapping == null || !StringUtils.hasText(text)) {
            return;
        }
        String fieldId = mapping.get(logicalKey);
        if (fieldId != null) {
            values.put(fieldId, WeComSmartSheetCellAdapter.textCell(text));
        }
    }

    private void putUrlValue(Map<String, Object> values, Map<String, String> mapping,
                             String logicalKey, String linkText, String link) {
        if (mapping == null || !StringUtils.hasText(link)) {
            return;
        }
        String fieldId = mapping.get(logicalKey);
        if (fieldId != null) {
            String displayText = StringUtils.hasText(linkText) ? linkText : link;
            values.put(fieldId, WeComSmartSheetCellAdapter.urlCell(displayText, link));
        }
    }

    private String resolveFirstSheetId(WeComGetSheetResponse sheetResponse) {
        if (sheetResponse.getSheetList() == null || sheetResponse.getSheetList().isEmpty()) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "新建智能表格未返回子表信息");
        }
        WeComSheetInfo sheet = sheetResponse.getSheetList().getFirst();
        return sheet.getSheetId();
    }

    private Map<String, String> copyColumnMapping(Map<String, String> source) {
        return source != null ? new HashMap<>(source) : new HashMap<>();
    }

    private LocalDate parseDate(String text) {
        if (!StringUtils.hasText(text)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private LocalTime parseTime(String text) {
        if (!StringUtils.hasText(text)) {
            return LocalTime.of(9, 0);
        }
        try {
            String normalized = text.trim();
            if (normalized.length() == 5) {
                return LocalTime.parse(normalized + ":00");
            }
            return LocalTime.parse(normalized);
        } catch (Exception e) {
            return LocalTime.of(9, 0);
        }
    }

    private int parseDuration(String text) {
        if (!StringUtils.hasText(text)) {
            return 30;
        }
        try {
            return Integer.parseInt(text.trim().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 30;
        }
    }

    private String mapSheetStatus(String sheetStatus) {
        if (sheetStatus.contains("取消")) {
            return MeetingRecordStatusEnum.CANCELLED.getCode();
        }
        if (sheetStatus.contains("结束") || sheetStatus.contains("完成")) {
            return MeetingRecordStatusEnum.COMPLETED.getCode();
        }
        if (sheetStatus.contains("进行")) {
            return MeetingRecordStatusEnum.IN_PROGRESS.getCode();
        }
        if (sheetStatus.contains("创建") || sheetStatus.contains("已创建")) {
            return MeetingRecordStatusEnum.SCHEDULED.getCode();
        }
        return MeetingRecordStatusEnum.PENDING.getCode();
    }
}
