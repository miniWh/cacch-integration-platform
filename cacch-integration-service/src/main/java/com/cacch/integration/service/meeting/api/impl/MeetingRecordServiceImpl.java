package com.cacch.integration.service.meeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.entity.meeting.MeetingRecordDO;
import com.cacch.integration.mapper.meeting.MeetingRecordMapper;
import com.cacch.integration.service.meeting.api.IMeetingRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 会议记录服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Service
@RequiredArgsConstructor
public class MeetingRecordServiceImpl implements IMeetingRecordService {

    private final MeetingRecordMapper meetingRecordMapper;

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public MeetingRecordDO getById(Long id) {
        return meetingRecordMapper.selectById(id);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public MeetingRecordDO getBySmartTableIdAndRecordId(Long smartTableId, String recordId) {
        return meetingRecordMapper.selectOne(new LambdaQueryWrapper<MeetingRecordDO>()
                .eq(MeetingRecordDO::getSmartTableId, smartTableId)
                .eq(MeetingRecordDO::getRecordId, recordId)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public List<MeetingRecordDO> listBySmartTableId(Long smartTableId) {
        return meetingRecordMapper.selectList(new LambdaQueryWrapper<MeetingRecordDO>()
                .eq(MeetingRecordDO::getSmartTableId, smartTableId));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public List<MeetingRecordDO> listByStatus(String status) {
        return meetingRecordMapper.selectList(new LambdaQueryWrapper<MeetingRecordDO>()
                .eq(MeetingRecordDO::getStatus, status));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public List<MeetingRecordDO> listByStatusOrEmpty(String status) {
        if (!StringUtils.hasText(status)) {
            return List.of();
        }
        return listByStatus(status);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void save(MeetingRecordDO record) {
        meetingRecordMapper.insert(record);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void updateById(MeetingRecordDO record) {
        meetingRecordMapper.updateById(record);
    }
}
