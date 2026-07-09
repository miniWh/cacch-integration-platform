package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.common.config.meeting.MeetingSyncProperties;
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
import com.cacch.integration.manager.meeting.api.IMeetingMinutesManager;
import com.cacch.integration.manager.meeting.api.IMeetingSyncManager;
import com.cacch.integration.manager.wecom.api.IWeComDocManager;
import com.cacch.integration.manager.wecom.api.IWeComMeetingManager;
import com.cacch.integration.manager.wecom.api.IWeComSmartSheetManager;
import com.cacch.integration.manager.wecom.api.IWeComUserManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import com.cacch.integration.service.meeting.api.IMeetingRecordService;
import com.cacch.integration.service.meeting.api.ISmartTableService;
import com.cacch.integration.service.meeting.api.ITodoItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final TodoSheetWriteSupport todoSheetWriteSupport;
    private final IWeComSmartSheetManager weComSmartSheetManager;
    private final IWeComDocManager weComDocManager;
    private final IWeComMeetingManager weComMeetingManager;
    private final IWeComUserManager weComUserManager;
    private final IWeComWebhookManager weComWebhookManager;
    private final IMeetingMinutesManager meetingMinutesManager;
    private final MeetingSyncTxSupport meetingSyncTxSupport;
    private final MeetingSyncProperties meetingSyncProperties;

    @Override
    public void scanMasterAndProvision() {
        SmartTableDO master = smartTableService.getEnabledMaster();
        if (master == null) {
            log.info("【MeetingSync】未配置启用的总控表(MASTER)，跳过扫描");
            return;
        }
        log.info("【MeetingSync】开始扫描总控表, docId={}", master.getDocId());
        long deadlineMs = resolveDeadlineMs();
        try {
            int pageSize = resolvePageSize(meetingSyncProperties.getMasterRecordBatchSize());
            WeComGetRecordsResponse recordsResponse = weComSmartSheetManager.getRecords(
                    master.getDocId(), master.getMeetingSheetId(), 0, pageSize);
            if (recordsResponse.getRecords() == null) {
                log.info("【MeetingSync】总控表无记录数据，跳过扫描, docId={}", master.getDocId());
                return;
            }
            List<WeComRecordInfo> records = limitBatch(
                    recordsResponse.getRecords(),
                    meetingSyncProperties.getMasterRecordBatchSize(),
                    "总控表扫描");
            Map<String, String> mapping = ensureMeetingColumnMapping(master);
            int processed = 0;
            for (WeComRecordInfo record : records) {
                if (isBudgetExceeded(deadlineMs, "总控表扫描")) {
                    break;
                }
                provisionEmployeeTable(master, mapping, record);
                processed++;
            }
            smartTableService.markSyncSuccess(master.getId());
            log.info("【MeetingSync】总控表扫描完成, docId={}, processed={}, fetched={}",
                    master.getDocId(), processed, records.size());
        } catch (Exception e) {
            smartTableService.markSyncError(master.getId(), e.getMessage());
            log.info("【MeetingSync】总控表扫描异常终止, masterId={}, docId={}, reason={}",
                    master.getId(), master.getDocId(), e.getMessage());
            log.error("【MeetingSync】总控表扫描失败, masterId={}, docId={}",
                    master.getId(), master.getDocId(), e);
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
        List<SmartTableDO> meetingTables = limitBatch(
                smartTableService.listEnabledMeetingTables(),
                meetingSyncProperties.getMeetingTableBatchSize(),
                "会议表扫描建会");
        long deadlineMs = resolveDeadlineMs();
        for (SmartTableDO table : meetingTables) {
            if (isBudgetExceeded(deadlineMs, "会议表扫描建会")) {
                break;
            }
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
        List<SmartTableDO> meetingTables = limitBatch(
                smartTableService.listEnabledMeetingTables(),
                meetingSyncProperties.getMeetingTableBatchSize(),
                "待发起建会");
        long deadlineMs = resolveDeadlineMs();
        int recordBudget = meetingSyncProperties.getMeetingRecordBatchSize();
        int processedRecords = 0;
        for (SmartTableDO table : meetingTables) {
            if (isBudgetExceeded(deadlineMs, "待发起建会")) {
                break;
            }
            List<MeetingRecordDO> records = meetingRecordService.listBySmartTableId(table.getId());
            for (MeetingRecordDO record : records) {
                if (recordBudget > 0 && processedRecords >= recordBudget) {
                    log.info("【MeetingSync】待发起建会提前结束, reason=达到会议记录批次上限, batchSize={}",
                            recordBudget);
                    return;
                }
                if (isBudgetExceeded(deadlineMs, "待发起建会")) {
                    return;
                }
                tryCreateWeComMeeting(table, record);
                processedRecords++;
            }
        }
    }

    @Override
    public void syncScheduledMeetingsFromWeCom() {
        List<MeetingRecordDO> records = limitBatch(
                meetingRecordService.listByStatusWithWecomMeetingId(
                        MeetingRecordStatusEnum.SCHEDULED.getCode()),
                meetingSyncProperties.getMeetingRecordBatchSize(),
                "企微会议详情反向同步");
        long deadlineMs = resolveDeadlineMs();
        int scanned = 0;
        int updated = 0;
        int unchanged = 0;
        int skippedStarted = 0;
        int failed = 0;
        for (MeetingRecordDO record : records) {
            if (isBudgetExceeded(deadlineMs, "企微会议详情反向同步")) {
                break;
            }
            scanned++;
            try {
                int outcome = trySyncScheduledMeetingFromWeCom(record);
                switch (outcome) {
                    case 1 -> updated++;
                    case -2 -> skippedStarted++;
                    case -1 -> failed++;
                    default -> unchanged++;
                }
            } catch (Exception e) {
                failed++;
                log.info("【MeetingSync】单条反向同步终止, recordId={}, meetingId={}, reason={}",
                        record.getRecordId(), record.getWecomMeetingId(), e.getMessage());
                log.error("【MeetingSync】企微会议详情反向同步失败, recordId={}, meetingId={}",
                        record.getRecordId(), record.getWecomMeetingId(), e);
            }
        }
        log.info("【MeetingSync】企微会议详情反向同步完成, scanned={}, updated={}, unchanged={}, skippedStarted={}, failed={}",
                scanned, updated, unchanged, skippedStarted, failed);
    }

    @Override
    public void syncTodosToSheet() {
        List<SmartTableDO> meetingTables = limitBatch(
                smartTableService.listEnabledMeetingTables(),
                meetingSyncProperties.getMeetingTableBatchSize(),
                "待办回写");
        long deadlineMs = resolveDeadlineMs();
        int todoBudget = meetingSyncProperties.getTodoBatchSize();
        int totalPending = 0;
        int totalSynced = 0;
        for (SmartTableDO table : meetingTables) {
            if (isBudgetExceeded(deadlineMs, "待办回写")) {
                break;
            }
            if (todoBudget > 0 && totalSynced >= todoBudget) {
                log.info("【MeetingSync】待办回写提前结束, reason=达到待办批次上限, batchSize={}", todoBudget);
                break;
            }
            if (!StringUtils.hasText(table.getTodoSheetId()) || table.getTodoColumnMapping() == null) {
                log.info("【MeetingSync】跳过待办回写, smartTableId={}, reason=待办子表未配置, todoSheetId={}, "
                                + "todoColumnMapping={}",
                        table.getId(), table.getTodoSheetId(), table.getTodoColumnMapping());
                continue;
            }
            List<TodoItemDO> todos = todoItemService.listPendingBySmartTableId(table.getId()).stream()
                    .filter(todo -> !StringUtils.hasText(todo.getRecordId()))
                    .toList();
            if (todos.isEmpty()) {
                log.info("【MeetingSync】跳过待办回写, smartTableId={}, reason=无待同步待办", table.getId());
                continue;
            }
            if (todoBudget > 0) {
                int remain = todoBudget - totalSynced;
                if (remain <= 0) {
                    break;
                }
                if (todos.size() > remain) {
                    log.info("【MeetingSync】待办回写批次截断, smartTableId={}, total={}, remain={}",
                            table.getId(), todos.size(), remain);
                    todos = todos.subList(0, remain);
                }
            }
            totalPending += todos.size();
            totalSynced += todoSheetWriteSupport.writeTodosToSheet(table, todos);
        }
        log.info("【MeetingSync】待办回写完成, pending={}, synced={}", totalPending, totalSynced);
    }

    @Override
    public void syncMeetingMinutesFromWeCom() {
        List<MeetingRecordDO> allRecords = meetingRecordService.listByStatusWithWecomMeetingId(
                MeetingRecordStatusEnum.SCHEDULED.getCode());
        if (allRecords.isEmpty()) {
            log.info("【MeetingSync】纪要拉取跳过, reason=无 SCHEDULED 且含企微会议ID 的记录");
            return;
        }
        List<MeetingRecordDO> records = limitBatch(
                allRecords, meetingSyncProperties.getMeetingRecordBatchSize(), "纪要拉取");
        log.info("【MeetingSync】开始纪要拉取, candidateCount={}, batchCount={}",
                allRecords.size(), records.size());
        long deadlineMs = resolveDeadlineMs();
        int scanned = 0;
        int processed = 0;
        int skipped = 0;
        int failed = 0;
        for (MeetingRecordDO record : records) {
            if (isBudgetExceeded(deadlineMs, "纪要拉取")) {
                break;
            }
            scanned++;
            try {
                int outcome = meetingMinutesManager.trySyncMinutes(record);
                switch (outcome) {
                    case 1 -> processed++;
                    case -1 -> failed++;
                    default -> skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.info("【MeetingSync】单条纪要拉取终止, recordId={}, meetingId={}, reason={}",
                        record.getRecordId(), record.getWecomMeetingId(), e.getMessage());
                log.error("【MeetingSync】纪要拉取失败, recordId={}, meetingId={}",
                        record.getRecordId(), record.getWecomMeetingId(), e);
            }
        }
        log.info("【MeetingSync】纪要拉取完成, scanned={}, processed={}, skipped={}, failed={}",
                scanned, processed, skipped, failed);
    }

    private long resolveDeadlineMs() {
        int maxRunSeconds = meetingSyncProperties.getMaxRunSeconds();
        if (maxRunSeconds <= 0) {
            return 0L;
        }
        return System.currentTimeMillis() + maxRunSeconds * 1000L;
    }

    private boolean isBudgetExceeded(long deadlineMs, String phase) {
        if (deadlineMs <= 0L || System.currentTimeMillis() < deadlineMs) {
            return false;
        }
        log.info("【MeetingSync】{}提前结束, reason=达到单次时间预算, maxRunSeconds={}",
                phase, meetingSyncProperties.getMaxRunSeconds());
        return true;
    }

    private int resolvePageSize(int batchSize) {
        if (batchSize <= 0) {
            return RECORD_PAGE_SIZE;
        }
        return Math.min(RECORD_PAGE_SIZE, batchSize);
    }

    private <T> List<T> limitBatch(List<T> source, int batchSize, String phase) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        if (batchSize <= 0 || source.size() <= batchSize) {
            return source;
        }
        log.info("【MeetingSync】{}批次截断, total={}, batchSize={}", phase, source.size(), batchSize);
        return source.subList(0, batchSize);
    }

    private void provisionEmployeeTable(SmartTableDO master, Map<String, String> mapping, WeComRecordInfo record) {
        Map<String, Object> values = record.getValues();
        String applyStatus = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "apply_status");
        if (!MeetingConstants.APPLY_STATUS_APPROVED.equals(applyStatus)) {
            log.info("【MeetingSync】跳过总控行建表, recordId={}, applyStatus={}, reason=申请未批准",
                    record.getRecordId(), applyStatus);
            return;
        }
        String createdDocId = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "created_doc_id");
        if (StringUtils.hasText(createdDocId)) {
            log.info("【MeetingSync】跳过总控行建表, recordId={}, createdDocId={}, reason=已创建会议管理表",
                    record.getRecordId(), createdDocId);
            return;
        }
        String userId = resolveMasterApplicantUserId(values, mapping);
        if (!StringUtils.hasText(userId)) {
            log.info("【MeetingSync】总控行缺少申请人，跳过建表, recordId={}", record.getRecordId());
            return;
        }
        String displayName = resolveMasterApplicantDisplayName(values, mapping, userId);
        String docName = StringUtils.hasText(displayName) ? displayName + "的会议管理" : userId + "的会议管理";

        SmartTableDO existingMeeting = smartTableService.getEnabledMeetingByUserId(userId);
        if (existingMeeting != null) {
            ensureEmployeeTableColumns(existingMeeting);
            writeBackMasterProvision(master, mapping, record.getRecordId(),
                    existingMeeting.getTableName() != null ? existingMeeting.getTableName() : docName,
                    existingMeeting.getDocId(), existingMeeting.getDocUrl());
            log.info("【MeetingSync】总控回写重试（本地已有会议表）, userId={}, docId={}",
                    userId, existingMeeting.getDocId());
            return;
        }

        WeComCreateDocResponse docResponse = weComDocManager.createSmartSheetDoc(docName, List.of(userId));
        WeComGetSheetResponse sheetResponse = weComSmartSheetManager.getSheets(docResponse.getDocid(), null, false);
        String meetingSheetId = resolveFirstSheetId(sheetResponse);
        weComSmartSheetManager.updateSheet(docResponse.getDocid(), meetingSheetId, MeetingConstants.MEETING_SHEET_TITLE);

        // 文档创建后尽早落库，避免后续列初始化瞬时失败时重复建文档。
        // meeting_column_mapping 为 NOT NULL，先写空映射，列初始化成功后再更新真实 fieldId。
        SmartTableDO meetingTable = new SmartTableDO();
        meetingTable.setTableType(SmartTableTypeEnum.MEETING.getCode());
        meetingTable.setUserId(userId);
        meetingTable.setTableName(docName);
        meetingTable.setDocId(docResponse.getDocid());
        meetingTable.setDocUrl(docResponse.getUrl());
        meetingTable.setMeetingSheetId(meetingSheetId);
        meetingTable.setMeetingColumnMapping(new HashMap<>());
        meetingTable.setStatus(MeetingConstants.SMART_TABLE_STATUS_ENABLED);
        smartTableService.saveNew(meetingTable);

        try {
            Map<String, String> meetingColumnMapping = setupMeetingSheetColumns(
                    docResponse.getDocid(), meetingSheetId);

            WeComAddSheetResponse todoSheetResponse = weComSmartSheetManager.addSheet(
                    docResponse.getDocid(), MeetingConstants.TODO_SHEET_TITLE, null);
            String todoSheetId = resolveTodoSheetId(todoSheetResponse);
            Map<String, String> todoColumnMapping = setupTodoSheetColumns(docResponse.getDocid(), todoSheetId);

            SmartTableDO update = new SmartTableDO();
            update.setId(meetingTable.getId());
            update.setMeetingColumnMapping(meetingColumnMapping);
            update.setTodoSheetId(todoSheetId);
            update.setTodoColumnMapping(todoColumnMapping);
            smartTableService.updateById(update);
            smartTableService.markSyncSuccess(meetingTable.getId());
        } catch (Exception e) {
            smartTableService.markSyncError(meetingTable.getId(), e.getMessage());
            log.info("【MeetingSync】员工会议表列初始化终止, userId={}, docId={}, reason={}",
                    userId, docResponse.getDocid(), e.getMessage());
            throw e;
        }

        writeBackMasterProvision(master, mapping, record.getRecordId(), docName,
                docResponse.getDocid(), docResponse.getUrl());
        log.info("【MeetingSync】为员工创建会议管理表, userId={}, docId={}", userId, docResponse.getDocid());
    }

    /**
     * 本地已有会议表但列映射缺失时，补齐会议/待办子表列（用于上次建表中途失败后的重试）。
     *
     * @param table 员工会议表配置，不可为空
     */
    private void ensureEmployeeTableColumns(SmartTableDO table) {
        boolean needMeetingColumns = table.getMeetingColumnMapping() == null
                || table.getMeetingColumnMapping().isEmpty();
        boolean needTodoSheet = !StringUtils.hasText(table.getTodoSheetId())
                || table.getTodoColumnMapping() == null
                || table.getTodoColumnMapping().isEmpty();
        if (!needMeetingColumns && !needTodoSheet) {
            return;
        }
        log.info("【MeetingSync】补齐员工会议表列, smartTableId={}, docId={}, needMeetingColumns={}, needTodoSheet={}",
                table.getId(), table.getDocId(), needMeetingColumns, needTodoSheet);
        try {
            SmartTableDO update = new SmartTableDO();
            update.setId(table.getId());
            if (needMeetingColumns) {
                Map<String, String> meetingColumnMapping = setupMeetingSheetColumns(
                        table.getDocId(), table.getMeetingSheetId());
                update.setMeetingColumnMapping(meetingColumnMapping);
                table.setMeetingColumnMapping(meetingColumnMapping);
            }
            if (needTodoSheet) {
                String todoSheetId = table.getTodoSheetId();
                if (!StringUtils.hasText(todoSheetId)) {
                    WeComAddSheetResponse todoSheetResponse = weComSmartSheetManager.addSheet(
                            table.getDocId(), MeetingConstants.TODO_SHEET_TITLE, null);
                    todoSheetId = resolveTodoSheetId(todoSheetResponse);
                    update.setTodoSheetId(todoSheetId);
                    table.setTodoSheetId(todoSheetId);
                }
                Map<String, String> todoColumnMapping = setupTodoSheetColumns(table.getDocId(), todoSheetId);
                update.setTodoColumnMapping(todoColumnMapping);
                table.setTodoColumnMapping(todoColumnMapping);
            }
            smartTableService.updateById(update);
            smartTableService.markSyncSuccess(table.getId());
        } catch (Exception e) {
            smartTableService.markSyncError(table.getId(), e.getMessage());
            log.info("【MeetingSync】补齐员工会议表列终止, smartTableId={}, docId={}, reason={}",
                    table.getId(), table.getDocId(), e.getMessage());
            throw e;
        }
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
                log.info("【MeetingSync】初始化列终止, smartTableId={}, docId={}, reason={}",
                        table.getId(), table.getDocId(), e.getMessage());
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

    private static final int WECOM_TRANSIENT_MAX_ATTEMPTS = 3;
    private static final long WECOM_TRANSIENT_RETRY_BASE_MS = 800L;

    /**
     * 初始化子表列：先遍历收集原始列，再按业务标题逆序新增列，最后删除原始列。
     * 删除默认列时对企微瞬时错误（errcode=-1）自动重试。
     */
    private Map<String, String> setupSheetColumns(String docId, String sheetId,
                                                  List<MeetingSheetColumnDef> columns,
                                                  Map<String, String> mapping) {
        WeComGetFieldsResponse fieldsResponse = weComSmartSheetManager.getFields(docId, sheetId, 0, 100);
        List<WeComFieldInfo> existingFields = fieldsResponse.getFields() != null
                ? fieldsResponse.getFields()
                : List.of();
        Set<String> desiredTitles = columns.stream()
                .map(MeetingSheetColumnDef::title)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        // 仅删除非业务列（新建文档默认列）；已存在的业务列保留，避免重试时重复新增
        List<String> originalFieldIds = existingFields.stream()
                .filter(field -> field.getFieldTitle() == null || !desiredTitles.contains(field.getFieldTitle()))
                .map(WeComFieldInfo::getFieldId)
                .toList();
        Set<String> existingTitles = existingFields.stream()
                .map(WeComFieldInfo::getFieldTitle)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        log.info("【MeetingSync】子表列初始化, docId={}, sheetId={}, 原始列数={}, 待删默认列数={}, 已有业务列数={}",
                docId, sheetId, existingFields.size(), originalFieldIds.size(), existingTitles.size());

        List<WeComFieldAddItem> toAdd = columns.reversed().stream()
                .filter(column -> !existingTitles.contains(column.title()))
                .map(this::toFieldAddItem)
                .toList();
        if (!toAdd.isEmpty()) {
            WeComAddFieldsResponse addResponse = weComSmartSheetManager.addFields(docId, sheetId, toAdd);
            if (addResponse.getFields() == null || addResponse.getFields().size() != toAdd.size()) {
                throw new BizException(ResultCode.INTEGRATION_ERROR, "添加子表字段返回不完整");
            }
        } else {
            log.info("【MeetingSync】子表业务列已齐全，跳过新增, docId={}, sheetId={}", docId, sheetId);
        }

        if (!originalFieldIds.isEmpty()) {
            deleteFieldsWithRetry(docId, sheetId, originalFieldIds);
        }

        log.info("【MeetingSync】子表列初始化完成, docId={}, sheetId={}, columns={}", docId, sheetId, mapping.keySet());
        return mapping;
    }

    /**
     * 删除字段，对企微瞬时不可用（errcode=-1 / temporarily unavailable）做有限次重试。
     *
     * @param docId    文档 ID
     * @param sheetId  子表 ID
     * @param fieldIds 待删除字段 ID 列表
     */
    private void deleteFieldsWithRetry(String docId, String sheetId, List<String> fieldIds) {
        BizException lastError = null;
        for (int attempt = 1; attempt <= WECOM_TRANSIENT_MAX_ATTEMPTS; attempt++) {
            try {
                weComSmartSheetManager.deleteFields(docId, sheetId, fieldIds);
                if (attempt > 1) {
                    log.info("【MeetingSync】删除默认列重试成功, docId={}, sheetId={}, attempt={}",
                            docId, sheetId, attempt);
                }
                return;
            } catch (BizException e) {
                lastError = e;
                if (!isTransientWeComError(e) || attempt == WECOM_TRANSIENT_MAX_ATTEMPTS) {
                    log.info("【MeetingSync】删除默认列终止, docId={}, sheetId={}, attempt={}, reason={}",
                            docId, sheetId, attempt, e.getMessage());
                    throw e;
                }
                long sleepMs = WECOM_TRANSIENT_RETRY_BASE_MS * attempt;
                log.info("【MeetingSync】删除默认列遇企微瞬时错误，准备重试, docId={}, sheetId={}, attempt={}/{}, sleepMs={}, reason={}",
                        docId, sheetId, attempt, WECOM_TRANSIENT_MAX_ATTEMPTS, sleepMs, e.getMessage());
                sleepQuietly(sleepMs);
            }
        }
        if (lastError != null) {
            throw lastError;
        }
    }

    /**
     * 判断是否为企微可重试的瞬时错误（如 errcode=-1 service temporarily unavailable）。
     *
     * @param e 业务异常
     * @return true 表示可重试
     */
    private boolean isTransientWeComError(BizException e) {
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("errcode=-1")
                || normalized.contains("temporarily unavailable")
                || normalized.contains("retry later")
                || normalized.contains("system busy")
                || normalized.contains("系统繁忙");
    }

    private void sleepQuietly(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.info("【MeetingSync】重试等待被中断, sleepMs={}", sleepMs);
            throw new BizException(ResultCode.SYSTEM_ERROR, "企微操作重试等待被中断", ie);
        }
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
            } else {
                log.info("【MeetingSync】会议子表无记录数据，跳过扫描, smartTableId={}, docId={}",
                        table.getId(), table.getDocId());
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
            log.info("【MeetingSync】扫描建会批次终止, smartTableId={}, docId={}, reason={}",
                    table.getId(), table.getDocId(), e.getMessage());
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
            log.info("【MeetingSync】跳过会议行同步, smartTableId={}, recordId={}, reason=会议标题为空",
                    table.getId(), row.getRecordId());
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
            log.info("【MeetingSync】单条建会终止, smartTableId={}, recordId={}, reason={}",
                    table.getId(), record.getRecordId(), e.getMessage());
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
            log.info("【MeetingSync】建会终止, recordId={}, reason=智能表格配置为空", record.getRecordId());
            return;
        }
        LocalDateTime startDateTime = LocalDateTime.of(record.getMeetingDate(), record.getStartTime());
        long epochSec = startDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        // 会议创建人 = 会议管理表「参会人」列第一人（admin_userid），不再使用表格归属人
        String creatorUserId = resolveMeetingCreatorUserId(record);
        log.info("【MeetingSync】准备创建企微会议, recordId={}, creator={}, attendees={}, tableOwner={}",
                record.getRecordId(), creatorUserId, record.getAttendees(), table.getUserId());
        WeComCreateMeetingResponse meetingResponse = weComMeetingManager.createMeeting(
                creatorUserId,
                record.getMeetingTitle(),
                epochSec,
                record.getDuration(),
                record.getAttendees(),
                record.getMeetingDescription(),
                record.getLocation());

        WeComGetMeetingInfoResponse info = weComMeetingManager.getMeetingInfo(meetingResponse.getMeetingid());
        String meetingCode = firstNonBlank(meetingResponse.getMeetingCode(), info.getMeetingCode());
        String meetingLink = firstNonBlank(meetingResponse.getMeetingLink(), info.getMeetingLink());
        meetingSyncTxSupport.markMeetingCreated(
                record, meetingResponse.getMeetingid(), meetingCode, meetingLink);
        writeBackMeetingStatus(table, record);
        log.info("【MeetingSync】创建企微会议成功, recordId={}, meetingId={}, creator={}",
                record.getId(), meetingResponse.getMeetingid(), creatorUserId);
    }

    /**
     * 解析会议创建人：取参会人列表第一人。
     *
     * @param record 会议记录，参会人不可为空
     * @return 创建人 userid
     */
    private String resolveMeetingCreatorUserId(MeetingRecordDO record) {
        if (record.getAttendees() == null || record.getAttendees().isEmpty()) {
            throw new BizException(ResultCode.PARAM_INVALID, "参会人为空，无法确定会议创建人");
        }
        for (String userId : record.getAttendees()) {
            if (StringUtils.hasText(userId)) {
                return userId.trim();
            }
        }
        throw new BizException(ResultCode.PARAM_INVALID, "参会人无效，无法确定会议创建人");
    }

    /**
     * @return 1 已更新，0 无变更，-1 失败，-2 已开始跳过
     */
    private int trySyncScheduledMeetingFromWeCom(MeetingRecordDO record) {
        SmartTableDO table = smartTableService.getById(record.getSmartTableId());
        if (table == null
                || !Integer.valueOf(MeetingConstants.SMART_TABLE_STATUS_ENABLED).equals(table.getStatus())) {
            log.info("【MeetingSync】跳过反向同步, recordId={}, smartTableId={}, reason=表格不存在或未启用",
                    record.getRecordId(), record.getSmartTableId());
            return 0;
        }
        if (!StringUtils.hasText(record.getRecordId()) || !StringUtils.hasText(record.getWecomMeetingId())) {
            log.info("【MeetingSync】跳过反向同步, recordId={}, meetingId={}, reason=缺少行ID或企微会议ID",
                    record.getRecordId(), record.getWecomMeetingId());
            return 0;
        }
        WeComGetMeetingInfoResponse info = weComMeetingManager.getMeetingInfo(record.getWecomMeetingId());
        if (isMeetingStarted(info, record)) {
            log.info("【MeetingSync】跳过已开始会议, recordId={}, meetingId={}",
                    record.getRecordId(), record.getWecomMeetingId());
            return -2;
        }
        Set<String> changedKeys = detectMeetingDetailChanges(record, info);
        if (changedKeys.isEmpty()) {
            log.info("【MeetingSync】跳过反向同步, recordId={}, meetingId={}, reason=企微详情无变更",
                    record.getRecordId(), record.getWecomMeetingId());
            return 0;
        }
        applyWeComMeetingDetails(record, info);
        meetingSyncTxSupport.updateMeetingAfterReverseSync(record);
        writeBackMeetingDetails(table, record, changedKeys);
        log.info("【MeetingSync】企微会议详情已回写表格, recordId={}, meetingId={}, fields={}",
                record.getRecordId(), record.getWecomMeetingId(), changedKeys);
        return 1;
    }

    private boolean isMeetingStarted(WeComGetMeetingInfoResponse info, MeetingRecordDO record) {
        LocalDateTime start = resolveMeetingStart(info, record);
        return start != null && !start.isAfter(LocalDateTime.now());
    }

    private LocalDateTime resolveMeetingStart(WeComGetMeetingInfoResponse info, MeetingRecordDO record) {
        if (info.getMeetingStart() != null && info.getMeetingStart() > 0) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(info.getMeetingStart()), ZoneId.systemDefault());
        }
        if (record.getMeetingDate() != null && record.getStartTime() != null) {
            return LocalDateTime.of(record.getMeetingDate(), record.getStartTime());
        }
        return null;
    }

    private Set<String> detectMeetingDetailChanges(MeetingRecordDO record, WeComGetMeetingInfoResponse info) {
        Set<String> changedKeys = new LinkedHashSet<>();
        if (!textEquals(record.getMeetingTitle(), info.getTitle())) {
            changedKeys.add("meeting_title");
        }
        if (!textEquals(record.getMeetingDescription(), info.getDescription())) {
            changedKeys.add("meeting_description");
        }
        if (!textEquals(record.getLocation(), info.getLocation())) {
            changedKeys.add("location");
        }
        LocalDateTime wecomStart = resolveMeetingStart(info, record);
        if (wecomStart != null && record.getMeetingDate() != null && record.getStartTime() != null) {
            LocalDateTime localStart = LocalDateTime.of(record.getMeetingDate(), record.getStartTime());
            if (!localStart.truncatedTo(ChronoUnit.MINUTES).equals(wecomStart.truncatedTo(ChronoUnit.MINUTES))) {
                changedKeys.add("start_time");
            }
        } else if (wecomStart != null) {
            changedKeys.add("start_time");
        }
        Integer wecomDurationMinutes = resolveDurationMinutes(info);
        if (!integerEquals(record.getDuration(), wecomDurationMinutes)) {
            changedKeys.add("duration");
        }
        List<String> wecomAttendees = info.getAttendees() != null
                ? info.getAttendees().extractMemberUserIds()
                : List.of();
        if (!attendeesEquals(record.getAttendees(), wecomAttendees)) {
            changedKeys.add("attendees");
        }
        if (changedKeys.contains("start_time") && resolveDurationMinutes(info) != null) {
            changedKeys.add("duration");
        }
        return changedKeys;
    }

    private void applyWeComMeetingDetails(MeetingRecordDO record, WeComGetMeetingInfoResponse info) {
        if (StringUtils.hasText(info.getTitle())) {
            record.setMeetingTitle(info.getTitle());
        }
        record.setMeetingDescription(normalizeText(info.getDescription()));
        record.setLocation(normalizeText(info.getLocation()));
        LocalDateTime start = resolveMeetingStart(info, record);
        if (start != null) {
            record.setMeetingDate(start.toLocalDate());
            record.setStartTime(start.toLocalTime());
        }
        Integer durationMinutes = resolveDurationMinutes(info);
        if (durationMinutes != null) {
            record.setDuration(durationMinutes);
        }
        if (info.getAttendees() != null) {
            List<String> attendees = info.getAttendees().extractMemberUserIds();
            if (!attendees.isEmpty()) {
                record.setAttendees(attendees);
            }
        }
    }

    /**
     * 根据企微开始/结束时间计算会议时长（分钟）。客户端编辑只有结束时间，无独立时长字段。
     */
    private Integer resolveDurationMinutes(WeComGetMeetingInfoResponse info) {
        Long startEpoch = info.getMeetingStart();
        Long endEpoch = resolveMeetingEndEpoch(info);
        if (startEpoch != null && endEpoch != null && endEpoch > startEpoch) {
            return (int) ((endEpoch - startEpoch) / 60);
        }
        return durationMinutesFromWeCom(info.getMeetingDuration());
    }

    private Long resolveMeetingEndEpoch(WeComGetMeetingInfoResponse info) {
        if (info.getMeetingEnd() != null && info.getMeetingEnd() > 0) {
            return info.getMeetingEnd();
        }
        if (info.getMeetingStart() != null && info.getMeetingDuration() != null && info.getMeetingDuration() > 0) {
            return info.getMeetingStart() + info.getMeetingDuration();
        }
        return null;
    }

    private Integer durationMinutesFromWeCom(Integer meetingDurationSec) {
        if (meetingDurationSec == null || meetingDurationSec <= 0) {
            return null;
        }
        return meetingDurationSec / 60;
    }

    private boolean textEquals(String left, String right) {
        return normalizeText(left).equals(normalizeText(right));
    }

    private String normalizeText(String text) {
        return text != null ? text.trim() : "";
    }

    private boolean integerEquals(Integer left, Integer right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private boolean attendeesEquals(List<String> left, List<String> right) {
        List<String> normalizedLeft = normalizeUserIds(left);
        List<String> normalizedRight = normalizeUserIds(right);
        return normalizedLeft.equals(normalizedRight);
    }

    private List<String> normalizeUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String userId : userIds) {
            if (userId != null && !userId.isBlank() && !normalized.contains(userId)) {
                normalized.add(userId);
            }
        }
        normalized.sort(String::compareTo);
        return normalized;
    }

    private void writeBackMeetingDetails(SmartTableDO table, MeetingRecordDO record, Set<String> changedKeys) {
        Map<String, String> mapping = ensureMeetingColumnMapping(table);
        if (mapping == null || changedKeys.isEmpty()) {
            log.info("【MeetingSync】跳过会议详情回写, recordId={}, reason=列映射为空或无变更字段",
                    record.getRecordId());
            return;
        }
        Map<String, Object> values = new HashMap<>();
        if (changedKeys.contains("meeting_title")) {
            putTextValue(values, mapping, "meeting_title", record.getMeetingTitle());
        }
        if (changedKeys.contains("meeting_description")) {
            putTextValue(values, mapping, "meeting_description", record.getMeetingDescription());
        }
        if (changedKeys.contains("location")) {
            putTextValue(values, mapping, "location", record.getLocation());
        }
        if (changedKeys.contains("start_time")
                && record.getMeetingDate() != null
                && record.getStartTime() != null) {
            putDateTimeValue(values, mapping, "start_time",
                    LocalDateTime.of(record.getMeetingDate(), record.getStartTime()));
        }
        if (changedKeys.contains("duration")) {
            putNumberValue(values, mapping, "duration", record.getDuration());
        }
        if (changedKeys.contains("attendees")) {
            putUsersValue(values, mapping, "attendees", record.getAttendees());
        }
        if (values.isEmpty()) {
            log.info("【MeetingSync】跳过会议详情回写, recordId={}, changedKeys={}, reason=无有效单元格值",
                    record.getRecordId(), changedKeys);
            return;
        }
        WeComRecordWriteItem item = WeComRecordWriteItem.builder()
                .recordId(record.getRecordId())
                .values(values)
                .build();
        weComSmartSheetManager.updateRecords(table.getDocId(), table.getMeetingSheetId(), List.of(item));
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

    /**
     * 从总控表行解析申请人 userId（人员列）；兼容旧映射 user_id 文本列。
     */
    private String resolveMasterApplicantUserId(Map<String, Object> values, Map<String, String> mapping) {
        List<String> userIds = WeComSmartSheetCellAdapter.getMappedUserIds(values, mapping, "applicant");
        if (!userIds.isEmpty()) {
            return userIds.get(0);
        }
        if (mapping != null && mapping.containsKey("user_id")) {
            return WeComSmartSheetCellAdapter.getMappedText(values, mapping, "user_id");
        }
        return null;
    }

    /**
     * 解析申请人展示名：人员列 name → 旧 user_name 文本列 → 通讯录 user/get。
     *
     * @param values  总控行单元格值
     * @param mapping 总控列映射
     * @param userId  申请人 userid，用于通讯录兜底查询
     * @return 员工姓名；均不可用时返回 null（建表名回退为 userid）
     */
    private String resolveMasterApplicantDisplayName(Map<String, Object> values, Map<String, String> mapping,
                                                     String userId) {
        String name = WeComSmartSheetCellAdapter.getMappedFirstUserDisplayName(values, mapping, "applicant");
        if (StringUtils.hasText(name)) {
            return name;
        }
        if (mapping != null && mapping.containsKey("user_name")) {
            String legacyName = WeComSmartSheetCellAdapter.getMappedText(values, mapping, "user_name");
            if (StringUtils.hasText(legacyName)) {
                return legacyName;
            }
        }
        String contactName = weComUserManager.getUserName(userId);
        if (!StringUtils.hasText(contactName)) {
            log.info("【MeetingSync】无法解析申请人姓名，建表名将使用userid, recordUserId={}", userId);
        }
        return contactName;
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
            log.info("【MeetingSync】跳过会议状态回写, recordId={}, meetingId={}, reason=列映射为空",
                    record.getRecordId(), record.getWecomMeetingId());
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
                                  String logicalKey, LocalDateTime dateTime) {
        if (mapping == null || dateTime == null) {
            return;
        }
        String dateTimeValue = WeComSmartSheetCellAdapter.dateTimeValue(dateTime);
        if (!StringUtils.hasText(dateTimeValue)) {
            return;
        }
        String fieldTitle = mapping.get(logicalKey);
        if (fieldTitle != null) {
            values.put(fieldTitle, dateTimeValue);
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

    private void putUsersValue(Map<String, Object> values, Map<String, String> mapping,
                               String logicalKey, List<String> userIds) {
        if (mapping == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        String fieldTitle = mapping.get(logicalKey);
        if (fieldTitle != null) {
            values.put(fieldTitle, WeComSmartSheetCellAdapter.userCells(userIds));
        }
    }

    private void putNumberValue(Map<String, Object> values, Map<String, String> mapping,
                                String logicalKey, Integer number) {
        if (mapping == null || number == null) {
            return;
        }
        String fieldTitle = mapping.get(logicalKey);
        if (fieldTitle != null) {
            values.put(fieldTitle, number);
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
            log.info("【MeetingSync】时间字段解析失败，使用兜底解析, raw={}, reason={}",
                    startText, e.getMessage());
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
