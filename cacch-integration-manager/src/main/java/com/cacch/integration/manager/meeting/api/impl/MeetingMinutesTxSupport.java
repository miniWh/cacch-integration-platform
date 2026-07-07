package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.common.enums.meeting.MeetingMinutesStatusEnum;
import com.cacch.integration.common.enums.meeting.MinutesFetchStatusEnum;
import com.cacch.integration.common.enums.meeting.TodoSourceEnum;
import com.cacch.integration.common.enums.meeting.TodoStatusEnum;
import com.cacch.integration.entity.meeting.MeetingMinutesDO;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.entity.meeting.SmartTableDO;
import com.cacch.integration.entity.meeting.TodoItemDO;
import com.cacch.integration.integration.wecom.adapter.WeComSmartSheetCellAdapter;
import com.cacch.integration.integration.wecom.client.dto.smartsheet.WeComRecordWriteItem;
import com.cacch.integration.manager.wecom.api.IWeComSmartSheetManager;
import com.cacch.integration.service.meeting.api.IMeetingMinutesService;
import com.cacch.integration.service.meeting.api.IMeetingRecordService;
import com.cacch.integration.service.meeting.api.ITodoItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会议纪要入库与表格回写（独立 Bean 保证事务生效）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MeetingMinutesTxSupport {

    private final IMeetingRecordService meetingRecordService;
    private final IMeetingMinutesService meetingMinutesService;
    private final ITodoItemService todoItemService;
    private final IWeComSmartSheetManager weComSmartSheetManager;

    /**
     * 持久化纪要解析结果并创建待办
     *
     * @param record     会议记录
     * @param table      智能表格配置
     * @param rawContent 纪要原文
     * @param todoTitles 待办标题列表
     * @return 新创建的待办数量
     */
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 60
    )
    public int persistMinutesAndTodos(MeetingRecordDO record, SmartTableDO table,
                                      String rawContent, List<String> todoTitles) {
        MeetingMinutesDO minutes = meetingMinutesService.getByMeetingId(record.getId());
        if (minutes == null) {
            minutes = new MeetingMinutesDO();
            minutes.setMeetingId(record.getId());
        }
        minutes.setRawContent(rawContent);
        minutes.setTodoList(buildTodoJson(todoTitles));
        minutes.setStatus(MinutesFetchStatusEnum.TODO_PARSED.getCode());
        minutes.setFetchTime(LocalDateTime.now());
        if (minutes.getId() == null) {
            meetingMinutesService.save(minutes);
        } else {
            meetingMinutesService.updateById(minutes);
        }
        int createdCount = 0;
        for (String title : todoTitles) {
            if (!StringUtils.hasText(title) || todoItemService.existsByMeetingIdAndTodoTitle(record.getId(), title)) {
                continue;
            }
            TodoItemDO todo = new TodoItemDO();
            todo.setMeetingId(record.getId());
            todo.setSmartTableId(record.getSmartTableId());
            todo.setTodoTitle(title);
            todo.setSource(TodoSourceEnum.FROM_MINUTES.getCode());
            todo.setStatus(TodoStatusEnum.PENDING.getCode());
            todoItemService.save(todo);
            createdCount++;
        }
        record.setMinutesStatus(MeetingMinutesStatusEnum.GENERATED.getCode());
        meetingRecordService.updateById(record);
        writeBackMinutesStatus(table, record);
        return createdCount;
    }

    /**
     * 无待办时标记纪要已处理，避免重复扫描
     *
     * @param record 会议记录
     * @param table  智能表格配置
     */
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 60
    )
    public void finalizeWithoutTodos(MeetingRecordDO record, SmartTableDO table) {
        record.setMinutesStatus(MeetingMinutesStatusEnum.GENERATED.getCode());
        meetingRecordService.updateById(record);
        writeBackMinutesStatus(table, record);
    }

    private List<Map<String, Object>> buildTodoJson(List<String> todoTitles) {
        List<Map<String, Object>> todoList = new ArrayList<>();
        for (String title : todoTitles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", title);
            todoList.add(item);
        }
        return todoList;
    }

    private void writeBackMinutesStatus(SmartTableDO table, MeetingRecordDO record) {
        Map<String, String> mapping = table.getMeetingColumnMapping();
        if (mapping == null) {
            return;
        }
        String fieldTitle = mapping.get("minutes_status");
        if (!StringUtils.hasText(fieldTitle)) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put(fieldTitle, WeComSmartSheetCellAdapter.textCell(
                MeetingMinutesStatusEnum.GENERATED.getDesc()));
        WeComRecordWriteItem item = WeComRecordWriteItem.builder()
                .recordId(record.getRecordId())
                .values(values)
                .build();
        weComSmartSheetManager.updateRecords(table.getDocId(), table.getMeetingSheetId(), List.of(item));
    }
}
