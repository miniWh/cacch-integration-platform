package com.cacch.integration.mapper.meeting;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.meeting.TodoItemDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoItemMapper extends BaseMapper<TodoItemDO> {
}
