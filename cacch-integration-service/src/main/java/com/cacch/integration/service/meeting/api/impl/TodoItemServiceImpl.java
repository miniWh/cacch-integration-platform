package com.cacch.integration.service.meeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
}
