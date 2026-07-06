package com.cacch.integration.mapper.meeting;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.meeting.TodoItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待办事项 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface TodoItemMapper extends BaseMapper<TodoItemDO> {
}
