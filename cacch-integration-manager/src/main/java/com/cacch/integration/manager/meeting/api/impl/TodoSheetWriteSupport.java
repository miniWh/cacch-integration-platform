package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.entity.meeting.SmartTableDO;
import com.cacch.integration.entity.meeting.TodoItemDO;
import com.cacch.integration.integration.wecom.adapter.WeComSmartSheetCellAdapter;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComAddRecordsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComFieldInfo;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComGetFieldsResponse;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordWriteItem;
import com.cacch.integration.manager.wecom.api.IWeComSmartSheetManager;
import com.cacch.integration.service.meeting.api.IMeetingRecordService;
import com.cacch.integration.service.meeting.api.ITodoItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 待办事项回写智能表格子表
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
class TodoSheetWriteSupport {

    private final IWeComSmartSheetManager weComSmartSheetManager;
    private final IMeetingRecordService meetingRecordService;
    private final ITodoItemService todoItemService;

    /**
     * 将待办列表写入待办子表
     *
     * @param table 智能表格配置
     * @param todos 待办列表
     * @return 成功写入子表的条数
     */
    int writeTodosToSheet(SmartTableDO table, List<TodoItemDO> todos) {
        if (todos == null || todos.isEmpty()) {
            return 0;
        }
        if (!canWriteTodoSheet(table)) {
            log.info("【TodoSheet】跳过待办回写, smartTableId={}, reason=待办子表未配置, todoSheetId={}, "
                            + "todoColumnMapping={}",
                    table.getId(), table.getTodoSheetId(), table.getTodoColumnMapping());
            return 0;
        }
        Map<String, String> mapping = resolveTodoColumnMapping(table);
        if (mapping == null || mapping.isEmpty()) {
            log.info("【TodoSheet】跳过待办回写, smartTableId={}, reason=待办列映射为空", table.getId());
            return 0;
        }
        int synced = 0;
        for (TodoItemDO todo : todos) {
            if (writeTodoToSheet(table, mapping, todo)) {
                synced++;
            }
        }
        log.info("【TodoSheet】待办回写完成, smartTableId={}, total={}, synced={}",
                table.getId(), todos.size(), synced);
        return synced;
    }

    private boolean canWriteTodoSheet(SmartTableDO table) {
        return table != null
                && StringUtils.hasText(table.getDocId())
                && StringUtils.hasText(table.getTodoSheetId())
                && table.getTodoColumnMapping() != null
                && !table.getTodoColumnMapping().isEmpty();
    }

    private boolean writeTodoToSheet(SmartTableDO table, Map<String, String> mapping, TodoItemDO todo) {
        TodoItemDO latest = todoItemService.getById(todo.getId());
        if (latest == null) {
            log.info("【TodoSheet】跳过待办回写, todoId={}, reason=待办不存在", todo.getId());
            return false;
        }
        if (StringUtils.hasText(latest.getRecordId())) {
            log.info("【TodoSheet】跳过待办回写, todoId={}, reason=已同步, sheetRecordId={}",
                    latest.getId(), latest.getRecordId());
            return false;
        }

        Map<String, Object> values = new HashMap<>();
        if (latest.getMeetingId() != null) {
            MeetingRecordDO meeting = meetingRecordService.getById(latest.getMeetingId());
            if (meeting != null) {
                putTextValue(values, mapping, "meeting_title", meeting.getMeetingTitle());
                putTextValue(values, mapping, "wecom_meeting_code", meeting.getWecomMeetingCode());
                if (meeting.getMeetingDate() != null && meeting.getStartTime() != null) {
                    putDateTimeValue(values, mapping, "start_time",
                            LocalDateTime.of(meeting.getMeetingDate(), meeting.getStartTime()));
                }
            }
        }
        putTextValue(values, mapping, "todo_item", latest.getTodoTitle());
        putUserValue(values, mapping, "assignee", latest.getAssignee());
        if (values.isEmpty()) {
            log.info("【TodoSheet】跳过待办回写, todoId={}, reason=无有效单元格值", latest.getId());
            return false;
        }

        WeComRecordWriteItem item = WeComRecordWriteItem.builder().values(values).build();
        var response = weComSmartSheetManager.addRecords(table.getDocId(), table.getTodoSheetId(), List.of(item));
        String sheetRecordId = resolveSheetRecordId(response);
        if (!StringUtils.hasText(sheetRecordId)) {
            log.warn("【TodoSheet】待办写入子表无返回 recordId, smartTableId={}, todoId={}, todoTitle={}",
                    table.getId(), latest.getId(), latest.getTodoTitle());
            return false;
        }
        if (!todoItemService.updateRecordIdIfAbsent(latest.getId(), sheetRecordId)) {
            log.info("【TodoSheet】待办已被其他流程同步, todoId={}, sheetRecordId={}",
                    latest.getId(), sheetRecordId);
            return false;
        }
        log.info("【TodoSheet】待办已写入子表, smartTableId={}, todoId={}, sheetRecordId={}, todoTitle={}",
                table.getId(), latest.getId(), sheetRecordId, latest.getTodoTitle());
        return true;
    }

    private String resolveSheetRecordId(WeComAddRecordsResponse response) {
        if (response == null || response.getRecords() == null || response.getRecords().isEmpty()) {
            return null;
        }
        return response.getRecords().getFirst().getRecordId();
    }

    private Map<String, String> resolveTodoColumnMapping(SmartTableDO table) {
        return resolveTitleBasedMapping(table, table.getTodoSheetId(), table.getTodoColumnMapping());
    }

    private Map<String, String> resolveTitleBasedMapping(SmartTableDO table, String sheetId,
                                                         Map<String, String> mapping) {
        if (mapping == null || mapping.isEmpty() || !StringUtils.hasText(sheetId)) {
            return mapping;
        }
        boolean needsResolve = mapping.values().stream().anyMatch(WeComSmartSheetCellAdapter::looksLikeFieldId);
        if (!needsResolve) {
            return mapping;
        }
        WeComGetFieldsResponse fieldsResponse = weComSmartSheetManager.getFields(
                table.getDocId(), sheetId, 0, 100);
        if (fieldsResponse.getFields() == null || fieldsResponse.getFields().isEmpty()) {
            log.info("【TodoSheet】企微子表列为空, smartTableId={}, sheetId={}", table.getId(), sheetId);
            return mapping;
        }
        Map<String, String> idToTitle = new HashMap<>();
        for (WeComFieldInfo field : fieldsResponse.getFields()) {
            idToTitle.put(field.getFieldId(), field.getFieldTitle());
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String value = entry.getValue();
            resolved.put(entry.getKey(), idToTitle.getOrDefault(value, value));
        }
        return resolved;
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
}
