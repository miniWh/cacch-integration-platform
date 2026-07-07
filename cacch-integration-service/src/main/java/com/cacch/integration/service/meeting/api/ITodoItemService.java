package com.cacch.integration.service.meeting.api;

import com.cacch.integration.entity.meeting.TodoItemDO;

import java.util.List;

/**
 * 待办事项服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ITodoItemService {

    /**
     * 查询会议关联的全部待办
     *
     * @param meetingId 会议记录主键
     * @return 待办列表
     */
    List<TodoItemDO> listByMeetingId(Long meetingId);

    /**
     * 查询智能表格下待回写的待办（状态为 PENDING）
     *
     * @param smartTableId 智能表格配置主键
     * @return 待办列表
     */
    List<TodoItemDO> listPendingBySmartTableId(Long smartTableId);

    /**
     * 新增待办
     *
     * @param todoItem 待保存实体
     */
    void save(TodoItemDO todoItem);

    /**
     * 按主键更新待办
     *
     * @param todoItem 待更新实体（须含 id）
     */
    void updateById(TodoItemDO todoItem);

    /**
     * 判断会议下是否已存在相同标题的待办（用于纪要解析去重）
     *
     * @param meetingId 会议记录主键
     * @param todoTitle 待办标题
     * @return 已存在时返回 true
     */
    boolean existsByMeetingIdAndTodoTitle(Long meetingId, String todoTitle);
}
