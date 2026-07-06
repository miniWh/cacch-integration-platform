package com.cacch.integration.mapper.meeting;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议记录 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MeetingRecordMapper extends BaseMapper<MeetingRecordDO> {
}
