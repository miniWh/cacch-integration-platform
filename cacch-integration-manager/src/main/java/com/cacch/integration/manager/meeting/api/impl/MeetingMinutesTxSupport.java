package com.cacch.integration.manager.meeting.api.impl;

import com.cacch.integration.common.enums.meeting.MeetingMinutesStatusEnum;
import com.cacch.integration.common.enums.meeting.MinutesFetchStatusEnum;
import com.cacch.integration.common.enums.meeting.TodoSourceEnum;
import com.cacch.integration.common.enums.meeting.TodoStatusEnum;
import com.cacch.integration.entity.meeting.MeetingMinutesDO;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.entity.meeting.TodoItemDO;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会议纪要数据库写操作支持（独立 Bean 保证事务生效；不含 HTTP 回写）
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

    /**
     * 持久化纪要解析结果并创建待办（仅 DB，不含表格回写）
     *
     * @param record     会议记录，不可为空
     * @param rawContent 纪要原文，可为空字符串
     * @param todoTitles 待办标题列表，不可为 null；空白与重复项会跳过
     * @return 新创建的待办数量
     */
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 60
    )
    public int persistMinutesAndTodos(MeetingRecordDO record, String rawContent, List<String> todoTitles) {
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
        return createdCount;
    }

    /**
     * 等待期内标记纪要待解析（仅 DB）
     *
     * @param record 会议记录，不可为空
     * @param reason 等待原因，用于日志
     * @return true 表示状态已更新；false 表示已是 PENDING，跳过
     */
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 60
    )
    public boolean markMinutesPending(MeetingRecordDO record, String reason) {
        if (MeetingMinutesStatusEnum.PENDING.getCode().equals(record.getMinutesStatus())) {
            log.info("【MeetingMinutes】跳过重复标记待解析, recordId={}, meetingId={}",
                    record.getRecordId(), record.getWecomMeetingId());
            return false;
        }
        log.info("【MeetingMinutes】纪要等待解析, recordId={}, meetingId={}, reason={}",
                record.getRecordId(), record.getWecomMeetingId(), reason);
        record.setMinutesStatus(MeetingMinutesStatusEnum.PENDING.getCode());
        meetingRecordService.updateById(record);
        return true;
    }

    /**
     * 纪要未获取到时标记处理完成（仅 DB，子表回写「无」由调用方在事务外执行）
     *
     * @param record 会议记录，不可为空
     * @param reason 未获取到纪要的原因，用于日志
     */
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 60
    )
    public void markMinutesNotObtained(MeetingRecordDO record, String reason) {
        log.info("【MeetingMinutes】纪要未获取, recordId={}, meetingId={}, reason={}",
                record.getRecordId(), record.getWecomMeetingId(), reason);
        record.setMinutesStatus(MeetingMinutesStatusEnum.GENERATED.getCode());
        meetingRecordService.updateById(record);
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
}
