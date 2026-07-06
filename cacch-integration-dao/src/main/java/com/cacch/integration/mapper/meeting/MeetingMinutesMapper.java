package com.cacch.integration.mapper.meeting;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.meeting.MeetingMinutesDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议纪要 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MeetingMinutesMapper extends BaseMapper<MeetingMinutesDO> {
}
