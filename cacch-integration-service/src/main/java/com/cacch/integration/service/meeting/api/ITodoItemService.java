package com.cacch.integration.service.meeting.api;

import com.cacch.integration.entity.meeting.TodoItemDO;

import java.util.List;

/**
 * 待办事项服务
 */
public interface ITodoItemService {

    List<TodoItemDO> listByMeetingId(Long meetingId);

    List<TodoItemDO> listPendingBySmartTableId(Long smartTableId);

    void save(TodoItemDO todoItem);

    void updateById(TodoItemDO todoItem);
}
