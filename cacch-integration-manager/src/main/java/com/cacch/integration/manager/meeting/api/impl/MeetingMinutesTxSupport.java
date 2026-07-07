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
        if (!StringUtils.hasText(rawContent)) {
            log.info("【MeetingMinutes】纪要原文为空，仍将标记已生成, recordId={}, meetingId={}",
                    record.getRecordId(), record.getWecomMeetingId());
        }
        MeetingMinutesDO minutes = meetingMinutesService.getByMeetingId(record.getId());
        if (minutes == null) {
            minutes = new MeetingMinutesDO();
            minutes.setMeetingId(record.getId());
            log.info("【MeetingMinutes】新建纪要记录, recordId={}, meetingId={}",
                    record.getRecordId(), record.getWecomMeetingId());
        } else {
            log.info("【MeetingMinutes】更新已有纪要记录, recordId={}, meetingId={}, minutesId={}",
                    record.getRecordId(), record.getWecomMeetingId(), minutes.getId());
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
        int skippedBlank = 0;
        int skippedDuplicate = 0;
        for (String title : todoTitles) {
            if (!StringUtils.hasText(title)) {
                skippedBlank++;
                continue;
            }
            if (todoItemService.existsByMeetingIdAndTodoTitle(record.getId(), title)) {
                skippedDuplicate++;
                log.info("【MeetingMinutes】跳过重复待办, recordId={}, meetingId={}, todoTitle={}",
                        record.getRecordId(), record.getWecomMeetingId(), title);
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
        if (skippedBlank > 0) {
            log.info("【MeetingMinutes】跳过空白待办标题, recordId={}, meetingId={}, count={}",
                    record.getRecordId(), record.getWecomMeetingId(), skippedBlank);
        }
        if (skippedDuplicate > 0) {
            log.info("【MeetingMinutes】跳过重复待办合计, recordId={}, meetingId={}, count={}",
                    record.getRecordId(), record.getWecomMeetingId(), skippedDuplicate);
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
     * @param reason 结束等待的原因
     */
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 60
    )
    public void finalizeWithoutTodos(MeetingRecordDO record, SmartTableDO table, String reason) {
        log.info("【MeetingMinutes】无待办结束处理, recordId={}, meetingId={}, reason={}",
                record.getRecordId(), record.getWecomMeetingId(), reason);
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
            log.info("【MeetingMinutes】跳过纪要状态回写, recordId={}, meetingId={}, reason=列映射为空",
                    record.getRecordId(), record.getWecomMeetingId());
            return;
        }
        String fieldTitle = mapping.get("minutes_status");
        if (!StringUtils.hasText(fieldTitle)) {
            log.info("【MeetingMinutes】跳过纪要状态回写, recordId={}, meetingId={}, reason=未配置 minutes_status 列",
                    record.getRecordId(), record.getWecomMeetingId());
            return;
        }
        if (!StringUtils.hasText(table.getDocId()) || !StringUtils.hasText(table.getMeetingSheetId())) {
            log.info("【MeetingMinutes】跳过纪要状态回写, recordId={}, meetingId={}, reason=docId 或 meetingSheetId 为空",
                    record.getRecordId(), record.getWecomMeetingId());
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
        log.info("【MeetingMinutes】纪要状态已回写表格, recordId={}, meetingId={}, status={}",
                record.getRecordId(), record.getWecomMeetingId(), MeetingMinutesStatusEnum.GENERATED.getDesc());
    }
}
