package com.cacch.integration.service.meeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cacch.integration.common.enums.meeting.TodoStatusEnum;
import com.cacch.integration.entity.meeting.TodoItemDO;
import com.cacch.integration.mapper.meeting.TodoItemMapper;
import com.cacch.integration.service.meeting.api.ITodoItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 待办事项服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Service
@RequiredArgsConstructor
public class TodoItemServiceImpl implements ITodoItemService {

    private final TodoItemMapper todoItemMapper;

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public List<TodoItemDO> listByMeetingId(Long meetingId) {
        return todoItemMapper.selectList(new LambdaQueryWrapper<TodoItemDO>()
                .eq(TodoItemDO::getMeetingId, meetingId));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public List<TodoItemDO> listPendingBySmartTableId(Long smartTableId) {
        return todoItemMapper.selectList(new LambdaQueryWrapper<TodoItemDO>()
                .eq(TodoItemDO::getSmartTableId, smartTableId)
                .eq(TodoItemDO::getStatus, TodoStatusEnum.PENDING.getCode()));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void save(TodoItemDO todoItem) {
        todoItemMapper.insert(todoItem);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void updateById(TodoItemDO todoItem) {
        todoItemMapper.updateById(todoItem);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public TodoItemDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return todoItemMapper.selectById(id);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public boolean updateRecordIdIfAbsent(Long id, String recordId) {
        if (id == null || recordId == null || recordId.isBlank()) {
            return false;
        }
        return todoItemMapper.update(null, new LambdaUpdateWrapper<TodoItemDO>()
                .eq(TodoItemDO::getId, id)
                .and(wrapper -> wrapper.isNull(TodoItemDO::getRecordId)
                        .or()
                        .eq(TodoItemDO::getRecordId, ""))
                .set(TodoItemDO::getRecordId, recordId.trim())) > 0;
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public boolean existsByMeetingIdAndTodoTitle(Long meetingId, String todoTitle) {
        if (meetingId == null || todoTitle == null || todoTitle.isBlank()) {
            return false;
        }
        return todoItemMapper.selectCount(new LambdaQueryWrapper<TodoItemDO>()
                .eq(TodoItemDO::getMeetingId, meetingId)
                .eq(TodoItemDO::getTodoTitle, todoTitle)) > 0;
    }
}
