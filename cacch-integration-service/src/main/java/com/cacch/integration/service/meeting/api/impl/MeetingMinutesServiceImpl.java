package com.cacch.integration.service.meeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.entity.meeting.MeetingMinutesDO;
import com.cacch.integration.mapper.meeting.MeetingMinutesMapper;
import com.cacch.integration.service.meeting.api.IMeetingMinutesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingMinutesServiceImpl implements IMeetingMinutesService {

    private final MeetingMinutesMapper meetingMinutesMapper;

    @Override
    public MeetingMinutesDO getByMeetingId(Long meetingId) {
        return meetingMinutesMapper.selectOne(new LambdaQueryWrapper<MeetingMinutesDO>()
                .eq(MeetingMinutesDO::getMeetingId, meetingId)
                .last("LIMIT 1"));
    }

    @Override
    public void save(MeetingMinutesDO minutes) {
        meetingMinutesMapper.insert(minutes);
    }

    @Override
    public void updateById(MeetingMinutesDO minutes) {
        meetingMinutesMapper.updateById(minutes);
    }
}
