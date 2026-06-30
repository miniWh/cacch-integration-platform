package com.cacch.integration.mapper.meeting;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.meeting.MeetingMinutesDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeetingMinutesMapper extends BaseMapper<MeetingMinutesDO> {
}
