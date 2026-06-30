package com.cacch.integration.service.meeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.mapper.meeting.MeetingRecordMapper;
import com.cacch.integration.service.meeting.api.IMeetingRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingRecordServiceImpl implements IMeetingRecordService {

    private final MeetingRecordMapper meetingRecordMapper;

    @Override
    public MeetingRecordDO getById(Long id) {
        return meetingRecordMapper.selectById(id);
    }

    @Override
    public MeetingRecordDO getBySmartTableIdAndRecordId(Long smartTableId, String recordId) {
        return meetingRecordMapper.selectOne(new LambdaQueryWrapper<MeetingRecordDO>()
                .eq(MeetingRecordDO::getSmartTableId, smartTableId)
                .eq(MeetingRecordDO::getRecordId, recordId)
                .last("LIMIT 1"));
    }

    @Override
    public List<MeetingRecordDO> listBySmartTableId(Long smartTableId) {
        return meetingRecordMapper.selectList(new LambdaQueryWrapper<MeetingRecordDO>()
                .eq(MeetingRecordDO::getSmartTableId, smartTableId));
    }

    @Override
    public List<MeetingRecordDO> listByStatus(String status) {
        return meetingRecordMapper.selectList(new LambdaQueryWrapper<MeetingRecordDO>()
                .eq(MeetingRecordDO::getStatus, status));
    }

    @Override
    public void save(MeetingRecordDO record) {
        meetingRecordMapper.insert(record);
    }

    @Override
    public void updateById(MeetingRecordDO record) {
        meetingRecordMapper.updateById(record);
    }
}
