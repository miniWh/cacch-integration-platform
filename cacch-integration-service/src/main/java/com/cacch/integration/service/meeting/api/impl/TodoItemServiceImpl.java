package com.cacch.integration.service.meeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.common.enums.meeting.TodoStatusEnum;
import com.cacch.integration.entity.meeting.TodoItemDO;
import com.cacch.integration.mapper.meeting.TodoItemMapper;
import com.cacch.integration.service.meeting.api.ITodoItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoItemServiceImpl implements ITodoItemService {

    private final TodoItemMapper todoItemMapper;

    @Override
    public List<TodoItemDO> listByMeetingId(Long meetingId) {
        return todoItemMapper.selectList(new LambdaQueryWrapper<TodoItemDO>()
                .eq(TodoItemDO::getMeetingId, meetingId));
    }

    @Override
    public List<TodoItemDO> listPendingBySmartTableId(Long smartTableId) {
        return todoItemMapper.selectList(new LambdaQueryWrapper<TodoItemDO>()
                .eq(TodoItemDO::getSmartTableId, smartTableId)
                .eq(TodoItemDO::getStatus, TodoStatusEnum.PENDING.getCode()));
    }

    @Override
    public void save(TodoItemDO todoItem) {
        todoItemMapper.insert(todoItem);
    }

    @Override
    public void updateById(TodoItemDO todoItem) {
        todoItemMapper.updateById(todoItem);
    }
}
