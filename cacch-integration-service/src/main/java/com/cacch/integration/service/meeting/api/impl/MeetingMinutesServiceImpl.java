package com.cacch.integration.service.meeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.entity.meeting.MeetingMinutesDO;
import com.cacch.integration.mapper.meeting.MeetingMinutesMapper;
import com.cacch.integration.service.meeting.api.IMeetingMinutesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会议纪要服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Service
@RequiredArgsConstructor
public class MeetingMinutesServiceImpl implements IMeetingMinutesService {

    private final MeetingMinutesMapper meetingMinutesMapper;

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public MeetingMinutesDO getByMeetingId(Long meetingId) {
        return meetingMinutesMapper.selectOne(new LambdaQueryWrapper<MeetingMinutesDO>()
                .eq(MeetingMinutesDO::getMeetingId, meetingId)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void save(MeetingMinutesDO minutes) {
        meetingMinutesMapper.insert(minutes);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void updateById(MeetingMinutesDO minutes) {
        meetingMinutesMapper.updateById(minutes);
    }
}
