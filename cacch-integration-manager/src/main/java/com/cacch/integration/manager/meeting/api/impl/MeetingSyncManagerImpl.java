package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.common.constant.meeting.MeetingConstants;
import com.cacch.integration.common.constant.meeting.MeetingSheetColumnDef;
import com.cacch.integration.common.constant.wecom.WeComConstants;
import com.cacch.integration.common.dto.meeting.MeetingCreateScanResult;
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
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComDateTimeFieldProperty;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComFieldAddItem;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComFieldInfo;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComNumberFieldProperty;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComSelectFieldProperty;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComSelectOption;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComSingleSelectFieldProperty;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUrlFieldProperty;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetSheetResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComUserFieldProperty;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
                log.warn("【MeetingSync】未填写已批准申请的总控表(MASTER)，跳过扫描");
                return;
            }
            Map<String, String> mapping = ensureMeetingColumnMapping(master);
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
        scanAndCreatePendingMeetings();
    }

    @Override
    public MeetingCreateScanResult scanAndCreatePendingMeetings() {
        MeetingCreateScanResult total = MeetingCreateScanResult.empty();
        List<SmartTableDO> meetingTables = smartTableService.listEnabledMeetingTables();
        for (SmartTableDO table : meetingTables) {
            total = total.merge(scanAndCreatePendingMeetingsForTable(table));
        }
        return total;
    }

    @Override
    public MeetingCreateScanResult scanAndCreatePendingMeetings(Long smartTableId) {
        SmartTableDO table = requireEnabledMeetingTable(smartTableId);
        return scanAndCreatePendingMeetingsForTable(table);
    }

    @Override
    public void createPendingWeComMeetings() {
        List<SmartTableDO> meetingTables = smartTableService.listEnabledMeetingTables();
        for (SmartTableDO table : meetingTables) {
            List<MeetingRecordDO> records = meetingRecordService.listBySmartTableId(table.getId());
            for (MeetingRecordDO record : records) {
                tryCreateWeComMeeting(table, record);
            }
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
            Map<String, String> todoMapping = ensureTodoColumnMapping(table);
            for (TodoItemDO todo : todos) {
                if (StringUtils.hasText(todo.getRecordId())) {
                    continue;
                }
                writeTodoToSheet(table, todoMapping, todo);
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

        weComSmartSheetManager.updateSheet(docResponse.getDocid(), meetingSheetId, MeetingConstants.MEETING_SHEET_TITLE);
        Map<String, String> meetingColumnMapping = setupMeetingSheetColumns(
                docResponse.getDocid(), meetingSheetId);

        WeComAddSheetResponse todoSheetResponse = weComSmartSheetManager.addSheet(
                docResponse.getDocid(), MeetingConstants.TODO_SHEET_TITLE, null);
        String todoSheetId = resolveTodoSheetId(todoSheetResponse);
        Map<String, String> todoColumnMapping = setupTodoSheetColumns(docResponse.getDocid(), todoSheetId);

        SmartTableDO meetingTable = new SmartTableDO();
        meetingTable.setTableType(SmartTableTypeEnum.MEETING.getCode());
        meetingTable.setUserId(userId);
        meetingTable.setTableName(docName);
        meetingTable.setDocId(docResponse.getDocid());
        meetingTable.setDocUrl(docResponse.getUrl());
        meetingTable.setMeetingSheetId(meetingSheetId);
        meetingTable.setMeetingColumnMapping(meetingColumnMapping);
        meetingTable.setTodoSheetId(todoSheetId);
        meetingTable.setTodoColumnMapping(todoColumnMapping);
        meetingTable.setStatus(MeetingConstants.SMART_TABLE_STATUS_ENABLED);
        smartTableService.saveNew(meetingTable);

        writeBackMasterProvision(master, mapping, record.getRecordId(), docName,
                docResponse.getDocid(), docResponse.getUrl());
        log.info("【MeetingSync】为员工创建会议管理表, userId={}, docId={}", userId, docResponse.getDocid());
    }

    @Override
    public void initializeMeetingSheetColumns(Long smartTableId) {
        SmartTableDO table = requireEnabledMeetingTable(smartTableId);
        doInitializeMeetingSheetColumns(table);
    }

    @Override
    public void initializeAllMeetingSheetColumns() {
        List<SmartTableDO> tables = smartTableService.listEnabledMeetingTables();
        for (SmartTableDO table : tables) {
            try {
                doInitializeMeetingSheetColumns(table);
            } catch (Exception e) {
                smartTableService.markSyncError(table.getId(), e.getMessage());
                log.error("【MeetingSync】手动初始化会议表列失败, smartTableId={}, docId={}",
                        table.getId(), table.getDocId(), e);
            }
        }
    }

    private SmartTableDO requireEnabledMeetingTable(Long smartTableId) {
        SmartTableDO table = smartTableService.getById(smartTableId);
        if (table == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "智能表格配置不存在, id=" + smartTableId);
        }
        if (!SmartTableTypeEnum.MEETING.getCode().equals(table.getTableType())) {
            throw new BizException(ResultCode.PARAM_INVALID, "仅支持 MEETING 类型员工会议表");
        }
        if (!Integer.valueOf(MeetingConstants.SMART_TABLE_STATUS_ENABLED).equals(table.getStatus())) {
            throw new BizException(ResultCode.PARAM_INVALID, "智能表格配置未启用, id=" + smartTableId);
        }
        return table;
    }

    private void doInitializeMeetingSheetColumns(SmartTableDO table) {
        log.info("【MeetingSync】手动初始化会议表列, smartTableId={}, docId={}", table.getId(), table.getDocId());
        Map<String, String> mapping = setupMeetingSheetColumns(table.getDocId(), table.getMeetingSheetId());
        SmartTableDO update = new SmartTableDO();
        update.setId(table.getId());
        update.setMeetingColumnMapping(mapping);
        smartTableService.updateById(update);
        smartTableService.markSyncSuccess(table.getId());
        log.info("【MeetingSync】手动初始化会议表列完成, smartTableId={}, columns={}", table.getId(), mapping.keySet());
    }

    /**
     * 初始化员工会议管理子表列，返回逻辑 key → 列标题 映射。
     */
    private Map<String, String> setupMeetingSheetColumns(String docId, String sheetId) {
        return setupSheetColumns(docId, sheetId, MeetingConstants.MEETING_SHEET_COLUMNS,
                MeetingConstants.buildMeetingColumnTitleMapping());
    }

    /**
     * 初始化会议待办事项子表列，返回逻辑 key → 列标题 映射。
     */
    private Map<String, String> setupTodoSheetColumns(String docId, String sheetId) {
        return setupSheetColumns(docId, sheetId, MeetingConstants.TODO_SHEET_COLUMNS,
                MeetingConstants.buildTodoColumnTitleMapping());
    }

    /**
     * 初始化子表列：先遍历收集原始列，再按业务标题逆序新增列，最后删除原始列。
     */
    private Map<String, String> setupSheetColumns(String docId, String sheetId,
                                                  List<MeetingSheetColumnDef> columns,
                                                  Map<String, String> mapping) {
        WeComGetFieldsResponse fieldsResponse = weComSmartSheetManager.getFields(docId, sheetId, 0, 100);
        List<WeComFieldInfo> existingFields = fieldsResponse.getFields() != null
                ? fieldsResponse.getFields()
                : List.of();
        List<String> originalFieldIds = existingFields.stream()
                .map(WeComFieldInfo::getFieldId)
                .toList();
        log.info("【MeetingSync】子表列初始化, docId={}, sheetId={}, 原始列数={}", docId, sheetId, originalFieldIds.size());

        List<WeComFieldAddItem> toAdd = columns.reversed().stream()
                .map(this::toFieldAddItem)
                .toList();
        WeComAddFieldsResponse addResponse = weComSmartSheetManager.addFields(docId, sheetId, toAdd);
        if (addResponse.getFields() == null || addResponse.getFields().size() != columns.size()) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "添加子表字段返回不完整");
        }

        if (!originalFieldIds.isEmpty()) {
            weComSmartSheetManager.deleteFields(docId, sheetId, originalFieldIds);
        }

        log.info("【MeetingSync】子表列初始化完成, docId={}, sheetId={}, columns={}", docId, sheetId, mapping.keySet());
        return mapping;
    }

    private WeComFieldAddItem toFieldAddItem(MeetingSheetColumnDef column) {
        WeComFieldAddItem.WeComFieldAddItemBuilder builder = WeComFieldAddItem.builder()
                .fieldTitle(column.title())
                .fieldType(column.fieldType());
        switch (column.fieldType()) {
            case WeComConstants.FIELD_TYPE_DATE_TIME -> builder.propertyDateTime(
                    WeComDateTimeFieldProperty.builder()
                            .format(WeComConstants.DATE_TIME_FORMAT_YMD_HM)
                            .autoFill(false)
                            .build());
            case WeComConstants.FIELD_TYPE_NUMBER -> builder.propertyNumber(
                    WeComNumberFieldProperty.builder()
                            .decimalPlaces(0)
                            .useSeparate(false)
                            .build());
            case WeComConstants.FIELD_TYPE_USER -> builder.propertyUser(
                    WeComUserFieldProperty.builder()
                            .isMultiple(!"assignee".equals(column.logicalKey()))
                            .build());
            case WeComConstants.FIELD_TYPE_SINGLE_SELECT -> builder.propertySingleSelect(
                    WeComSingleSelectFieldProperty.builder()
                            .isQuickAdd(false)
                            .options(column.selectOptions().stream()
                                    .map(text -> WeComSelectOption.builder().text(text).build())
                                    .toList())
                            .build());
            case WeComConstants.FIELD_TYPE_SELECT -> builder.propertySelect(
                    WeComSelectFieldProperty.builder()
                            .isQuickAdd(true)
                            .options(column.selectOptions().stream()
                                    .map(text -> WeComSelectOption.builder().text(text).build())
                                    .toList())
                            .build());
            case WeComConstants.FIELD_TYPE_URL -> builder.propertyUrl(
                    WeComUrlFieldProperty.builder()
                            .type(WeComConstants.URL_LINK_TYPE_PURE_TEXT)
                            .build());
            default -> {
            }
        }
        return builder.build();
    }

    private MeetingCreateScanResult scanAndCreatePendingMeetingsForTable(SmartTableDO table) {
        log.info("【MeetingSync】扫描会议管理子表并尝试建会, smartTableId={}, docId={}", table.getId(), table.getDocId());
        int scannedRows = 0;
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        try {
            WeComGetRecordsResponse response = weComSmartSheetManager.getRecords(
                    table.getDocId(), table.getMeetingSheetId(), 0, RECORD_PAGE_SIZE);
            if (response.getRecords() != null) {
                Map<String, String> mapping = ensureMeetingColumnMapping(table);
                for (WeComRecordInfo row : response.getRecords()) {
                    scannedRows++;
                    MeetingRecordDO record = upsertMeetingRecord(table, mapping, row);
                    if (record == null) {
                        skippedCount++;
                        continue;
                    }
                    int outcome = tryCreateWeComMeeting(table, record);
                    switch (outcome) {
                        case 1 -> createdCount++;
                        case -1 -> failedCount++;
                        default -> skippedCount++;
                    }
                }
            }
            smartTableService.markSyncSuccess(table.getId());
        } catch (Exception e) {
            smartTableService.markSyncError(table.getId(), e.getMessage());
            weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                    .biz(BIZ)
                    .title("智能表格扫描建会异常")
                    .subject(table.getTableName())
                    .context("smartTableId=" + table.getId() + ", docId=" + table.getDocId())
                    .error(e)
                    .dedupType("table")
                    .dedupId(String.valueOf(table.getId()))
                    .build());
            log.error("【MeetingSync】扫描建会失败, smartTableId={}", table.getId(), e);
            throw e;
        }
        log.info("【MeetingSync】扫描建会完成, smartTableId={}, scanned={}, created={}, skipped={}, failed={}",
                table.getId(), scannedRows, createdCount, skippedCount, failedCount);
        return new MeetingCreateScanResult(scannedRows, createdCount, skippedCount, failedCount);
    }

    private MeetingRecordDO upsertMeetingRecord(SmartTableDO table, Map<String, String> mapping, WeComRecordInfo row) {
        Map<String, Object> values = row.getValues();
        String title = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "meeting_title");
        if (!StringUtils.hasText(title)) {
            log.warn("【MeetingSync】会议标题为空, smartTableId={}, recordId={}", table.getId(), row.getRecordId());
            return null;
        }
        MeetingRecordDO existing = meetingRecordService.getBySmartTableIdAndRecordId(table.getId(), row.getRecordId());
        MeetingRecordDO record = existing != null ? existing : new MeetingRecordDO();
        record.setSmartTableId(table.getId());
        record.setRecordId(row.getRecordId());
        record.setMeetingTitle(title);
        List<String> meetingTypes = WeComSmartSheetCellAdapter.getMappedSelectTexts(values, mapping, "meeting_type");
        if (!meetingTypes.isEmpty()) {
            record.setMeetingType(meetingTypes);
        }
        List<String> meetingTopics = WeComSmartSheetCellAdapter.getMappedSelectTexts(values, mapping, "meeting_topics");
        if (!meetingTopics.isEmpty()) {
            record.setMeetingTopics(meetingTopics);
        }
        record.setMeetingDescription(
                WeComSmartSheetCellAdapter.getMappedText(values, mapping, "meeting_description"));
        record.setLocation(WeComSmartSheetCellAdapter.getMappedText(values, mapping, "location"));
        applyMeetingStart(record, WeComSmartSheetCellAdapter.getMappedDateTime(values, mapping, "start_time"));
        record.setDuration(WeComSmartSheetCellAdapter.getMappedNumber(values, mapping, "duration"));
        record.setMeetingLink(WeComSmartSheetCellAdapter.getMappedText(values, mapping, "meeting_link"));
        List<String> attendees = WeComSmartSheetCellAdapter.getMappedUserIds(values, mapping, "attendees");
        if (!attendees.isEmpty()) {
            record.setAttendees(attendees);
        }
        String sheetStatus = WeComSmartSheetCellAdapter.getMappedSelectText(values, mapping, "status");
        String sheetMinutesStatus = WeComSmartSheetCellAdapter.getMappedSelectText(values, mapping, "minutes_status");
        String sheetWecomMeetingCode = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "wecom_meeting_code");
        String sheetWecomMeetingId = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "wecom_meeting_id");
        if (StringUtils.hasText(sheetWecomMeetingCode)) {
            record.setWecomMeetingCode(sheetWecomMeetingCode);
        }
        if (StringUtils.hasText(sheetWecomMeetingId)) {
            record.setWecomMeetingId(sheetWecomMeetingId);
        }
        if (StringUtils.hasText(sheetStatus)) {
            record.setStatus(mapSheetStatus(sheetStatus));
        } else if (existing == null) {
            record.setStatus(null);
        }
        if (StringUtils.hasText(sheetMinutesStatus)) {
            record.setMinutesStatus(mapSheetMinutesStatus(sheetMinutesStatus));
        } else if (existing == null) {
            record.setMinutesStatus(MeetingMinutesStatusEnum.NONE.getCode());
        }
        if (existing == null) {
            meetingRecordService.save(record);
        } else {
            meetingRecordService.updateById(record);
        }
        return record;
    }

    /**
     * @return 1 已创建，0 跳过，-1 失败
     */
    private int tryCreateWeComMeeting(SmartTableDO table, MeetingRecordDO record) {
        if (!isEligibleForMeetingCreation(record)) {
            return 0;
        }
        try {
            createWeComMeetingForRecord(table, record);
            return 1;
        } catch (Exception e) {
            log.error("【MeetingSync】创建企微会议失败, smartTableId={}, recordId={}",
                    table.getId(), record.getRecordId(), e);
            return -1;
        }
    }

    /**
     * 校验是否满足自动建会条件：会议状态为「待发起」，且主题/时间/时长/参会人均合法。
     */
    private boolean isEligibleForMeetingCreation(MeetingRecordDO record) {
        if (!StringUtils.hasText(record.getStatus())) {
            log.info("【MeetingSync】跳过建会: 未选择会议状态, recordId={}", record.getRecordId());
            return false;
        }
        if (MeetingRecordStatusEnum.SCHEDULED.getCode().equals(record.getStatus())
                || StringUtils.hasText(record.getWecomMeetingId())) {
            log.info("【MeetingSync】跳过建会: 会议已是已创建, recordId={}", record.getRecordId());
            return false;
        }
        if (!MeetingRecordStatusEnum.PENDING.getCode().equals(record.getStatus())) {
            log.info("【MeetingSync】跳过建会: 会议状态非待发起, status={}, recordId={}",
                    record.getStatus(), record.getRecordId());
            return false;
        }
        if (!StringUtils.hasText(record.getMeetingTitle())) {
            log.info("【MeetingSync】跳过建会: 会议主题为空, recordId={}", record.getRecordId());
            return false;
        }
        if (record.getMeetingDate() == null || record.getStartTime() == null) {
            log.info("【MeetingSync】跳过建会: 开始时间为空, recordId={}", record.getRecordId());
            return false;
        }
        LocalDateTime startDateTime = LocalDateTime.of(record.getMeetingDate(), record.getStartTime());
        if (!startDateTime.isAfter(LocalDateTime.now())) {
            log.info("【MeetingSync】跳过建会: 开始时间不晚于当前时间, recordId={}", record.getRecordId());
            return false;
        }
        if (record.getDuration() == null
                || record.getDuration() < MeetingConstants.MIN_MEETING_DURATION_MINUTES) {
            log.info("【MeetingSync】跳过建会: 会议时长不足{}分钟, recordId={}",
                    MeetingConstants.MIN_MEETING_DURATION_MINUTES, record.getRecordId());
            return false;
        }
        if (record.getAttendees() == null || record.getAttendees().isEmpty()) {
            log.info("【MeetingSync】跳过建会: 参会人为空, recordId={}", record.getRecordId());
            return false;
        }
        return true;
    }

    private void createWeComMeetingForRecord(SmartTableDO table, MeetingRecordDO record) {
        if (table == null) {
            log.warn("【MeetingSync】创建企微会议失败, 未配置智能表格, recordId={}", record.getRecordId());
            return;
        }
        LocalDateTime startDateTime = LocalDateTime.of(record.getMeetingDate(), record.getStartTime());
        long epochSec = startDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        String hostUserId = record.getAttendees().getFirst();
        WeComCreateMeetingResponse meetingResponse = weComMeetingManager.createMeeting(
                hostUserId,
                record.getMeetingTitle(),
                epochSec,
                record.getDuration(),
                record.getAttendees(),
                record.getMeetingDescription(),
                record.getLocation());

        WeComGetMeetingInfoResponse info = weComMeetingManager.getMeetingInfo(meetingResponse.getMeetingid());
        record.setWecomMeetingId(meetingResponse.getMeetingid());
        record.setWecomMeetingCode(firstNonBlank(meetingResponse.getMeetingCode(), info.getMeetingCode()));
        record.setMeetingLink(firstNonBlank(meetingResponse.getMeetingLink(), info.getMeetingLink()));
        record.setStatus(MeetingRecordStatusEnum.SCHEDULED.getCode());
        meetingRecordService.updateById(record);

        writeBackMeetingStatus(table, record);
        log.info("【MeetingSync】创建企微会议成功, recordId={}, meetingId={}, host={}",
                record.getId(), meetingResponse.getMeetingid(), hostUserId);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary;
        }
        return StringUtils.hasText(fallback) ? fallback : null;
    }

    private Map<String, String> ensureMeetingColumnMapping(SmartTableDO table) {
        return ensureTitleBasedMapping(table, table.getMeetingSheetId(), table.getMeetingColumnMapping(), true);
    }

    private Map<String, String> ensureTodoColumnMapping(SmartTableDO table) {
        return ensureTitleBasedMapping(table, table.getTodoSheetId(), table.getTodoColumnMapping(), false);
    }

    /**
     * 将历史 fieldId 映射自动解析为列标题并回写 DB，便于用户删列重建后列名不变即可继续同步。
     */
    private Map<String, String> ensureTitleBasedMapping(SmartTableDO table, String sheetId,
                                                        Map<String, String> mapping, boolean meetingMapping) {
        if (mapping == null || mapping.isEmpty() || !StringUtils.hasText(sheetId)) {
            log.info("【MeetingSync】列映射为空, smartTableId={}, sheetId={}", table.getId(), sheetId);
            return mapping;
        }
        boolean needsResolve = mapping.values().stream().anyMatch(WeComSmartSheetCellAdapter::looksLikeFieldId);
        if (!needsResolve) {
            log.debug("【MeetingSync】列映射已为列标题，无需 fieldId 迁移, smartTableId={}", table.getId());
            return mapping;
        }
        WeComGetFieldsResponse fieldsResponse = weComSmartSheetManager.getFields(
                table.getDocId(), sheetId, 0, 100);
        if (fieldsResponse.getFields() == null || fieldsResponse.getFields().isEmpty()) {
            log.info("【MeetingSync】企微列为空为空, smartTableId={}, sheetId={}", table.getId(), sheetId);
            return mapping;
        }
        Map<String, String> idToTitle = new HashMap<>();
        for (WeComFieldInfo field : fieldsResponse.getFields()) {
            idToTitle.put(field.getFieldId(), field.getFieldTitle());
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String value = entry.getValue();
            String title = idToTitle.getOrDefault(value, value);
            resolved.put(entry.getKey(), title);
            if (!title.equals(value)) {
                changed = true;
            }
        }
        if (changed) {
            SmartTableDO update = new SmartTableDO();
            update.setId(table.getId());
            if (meetingMapping) {
                update.setMeetingColumnMapping(resolved);
                table.setMeetingColumnMapping(resolved);
            } else {
                update.setTodoColumnMapping(resolved);
                table.setTodoColumnMapping(resolved);
            }
            smartTableService.updateById(update);
            log.info("【MeetingSync】列映射已从 fieldId 迁移为列标题, smartTableId={}, meeting={}",
                    table.getId(), meetingMapping);
        }
        return resolved;
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
        Map<String, String> mapping = ensureMeetingColumnMapping(table);
        if (mapping == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        putTextValue(values, mapping, "status", MeetingRecordStatusEnum.SCHEDULED.getDesc());
        if (StringUtils.hasText(record.getMeetingLink())) {
            putUrlValue(values, mapping, "meeting_link", "会议链接", record.getMeetingLink());
        }
        if (StringUtils.hasText(record.getWecomMeetingCode())) {
            putTextValue(values, mapping, "wecom_meeting_code", record.getWecomMeetingCode());
        }
        if (StringUtils.hasText(record.getWecomMeetingId())) {
            putTextValue(values, mapping, "wecom_meeting_id", record.getWecomMeetingId());
        }
        WeComRecordWriteItem item = WeComRecordWriteItem.builder()
                .recordId(record.getRecordId())
                .values(values)
                .build();
        weComSmartSheetManager.updateRecords(table.getDocId(), table.getMeetingSheetId(), List.of(item));
    }

    private void writeTodoToSheet(SmartTableDO table, Map<String, String> mapping, TodoItemDO todo) {
        Map<String, Object> values = new HashMap<>();
        if (todo.getMeetingId() != null) {
            MeetingRecordDO meeting = meetingRecordService.getById(todo.getMeetingId());
            if (meeting != null) {
                putTextValue(values, mapping, "meeting_title", meeting.getMeetingTitle());
                putTextValue(values, mapping, "wecom_meeting_code", meeting.getWecomMeetingCode());
                if (meeting.getMeetingDate() != null && meeting.getStartTime() != null) {
                    String startDateTime = meeting.getMeetingDate() + " "
                            + meeting.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    putDateTimeValue(values, mapping, "start_time", startDateTime);
                }
            }
        }
        putTextValue(values, mapping, "todo_item", todo.getTodoTitle());
        putUserValue(values, mapping, "assignee", todo.getAssignee());

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
        String fieldTitle = mapping.get(logicalKey);
        if (fieldTitle != null) {
            values.put(fieldTitle, WeComSmartSheetCellAdapter.textCell(text));
        }
    }

    private void putUrlValue(Map<String, Object> values, Map<String, String> mapping,
                             String logicalKey, String linkText, String link) {
        if (mapping == null || !StringUtils.hasText(link)) {
            return;
        }
        String fieldTitle = mapping.get(logicalKey);
        if (fieldTitle != null) {
            String displayText = StringUtils.hasText(linkText) ? linkText : link;
            values.put(fieldTitle, WeComSmartSheetCellAdapter.urlCell(displayText, link));
        }
    }

    private void putDateTimeValue(Map<String, Object> values, Map<String, String> mapping,
                                  String logicalKey, String dateTime) {
        if (mapping == null || !StringUtils.hasText(dateTime)) {
            return;
        }
        String fieldTitle = mapping.get(logicalKey);
        if (fieldTitle != null) {
            values.put(fieldTitle, dateTime);
        }
    }

    private void putUserValue(Map<String, Object> values, Map<String, String> mapping,
                              String logicalKey, String userId) {
        if (mapping == null || !StringUtils.hasText(userId)) {
            return;
        }
        String fieldTitle = mapping.get(logicalKey);
        if (fieldTitle != null) {
            values.put(fieldTitle, WeComSmartSheetCellAdapter.userCell(userId));
        }
    }

    private String resolveFirstSheetId(WeComGetSheetResponse sheetResponse) {
        if (sheetResponse.getSheetList() == null || sheetResponse.getSheetList().isEmpty()) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "新建智能表格未返回子表信息");
        }
        WeComSheetInfo sheet = sheetResponse.getSheetList().getFirst();
        return sheet.getSheetId();
    }

    private String resolveTodoSheetId(WeComAddSheetResponse response) {
        if (response.getProperties() == null || !StringUtils.hasText(response.getProperties().getSheetId())) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "新建待办子表未返回 sheetId");
        }
        return response.getProperties().getSheetId();
    }

    private void applyMeetingStart(MeetingRecordDO record, LocalDateTime dateTime) {
        if (dateTime == null) {
            return;
        }
        record.setMeetingDate(dateTime.toLocalDate());
        record.setStartTime(dateTime.toLocalTime());
    }

    private void applyMeetingStart(MeetingRecordDO record, String startText) {
        if (!StringUtils.hasText(startText)) {
            record.setMeetingDate(LocalDate.now());
            record.setStartTime(LocalTime.of(9, 0));
            return;
        }
        String normalized = startText.trim().replace('T', ' ');
        try {
            if (normalized.length() >= 16) {
                String dateTimePart = normalized.length() >= 19 ? normalized.substring(0, 19) : normalized.substring(0, 16);
                DateTimeFormatter formatter = normalized.length() >= 19
                        ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(dateTimePart, formatter);
                record.setMeetingDate(dateTime.toLocalDate());
                record.setStartTime(dateTime.toLocalTime());
                return;
            }
            long epoch = Long.parseLong(normalized);
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(epoch > 1_000_000_000_000L ? epoch / 1000 : epoch),
                    ZoneId.systemDefault());
            record.setMeetingDate(dateTime.toLocalDate());
            record.setStartTime(dateTime.toLocalTime());
        } catch (DateTimeParseException | NumberFormatException e) {
            record.setMeetingDate(parseDate(normalized));
            record.setStartTime(parseTime(normalized));
        }
    }

    private String mapSheetMinutesStatus(String sheetStatus) {
        for (MeetingMinutesStatusEnum statusEnum : MeetingMinutesStatusEnum.values()) {
            if (sheetStatus.equals(statusEnum.getDesc()) || sheetStatus.contains(statusEnum.getDesc())) {
                return statusEnum.getCode();
            }
        }
        return MeetingMinutesStatusEnum.NONE.getCode();
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

    private Integer parseDuration(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String mapSheetStatus(String sheetStatus) {
        for (MeetingRecordStatusEnum statusEnum : MeetingRecordStatusEnum.values()) {
            if (sheetStatus.equals(statusEnum.getDesc()) || sheetStatus.contains(statusEnum.getDesc())) {
                return statusEnum.getCode();
            }
        }
        if (sheetStatus.contains("取消")) {
            return MeetingRecordStatusEnum.CANCELLED.getCode();
        }
        if (sheetStatus.contains("创建") || sheetStatus.contains("已创建")) {
            return MeetingRecordStatusEnum.SCHEDULED.getCode();
        }
        return MeetingRecordStatusEnum.PENDING.getCode();
    }
}
