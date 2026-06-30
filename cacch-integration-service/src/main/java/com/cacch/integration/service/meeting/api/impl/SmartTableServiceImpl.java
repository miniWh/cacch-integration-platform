package com.cacch.integration.service.meeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.common.enums.meeting.SmartTableTypeEnum;
import com.cacch.integration.entity.meeting.SmartTableDO;
import com.cacch.integration.mapper.meeting.SmartTableMapper;
import com.cacch.integration.service.meeting.api.ISmartTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SmartTableServiceImpl implements ISmartTableService {

    private static final int STATUS_ENABLED = 1;

    private final SmartTableMapper smartTableMapper;

    @Override
    public SmartTableDO getById(Long id) {
        return smartTableMapper.selectById(id);
    }

    @Override
    public SmartTableDO getEnabledMaster() {
        return smartTableMapper.selectOne(new LambdaQueryWrapper<SmartTableDO>()
                .eq(SmartTableDO::getTableType, SmartTableTypeEnum.MASTER.getCode())
                .eq(SmartTableDO::getStatus, STATUS_ENABLED)
                .last("LIMIT 1"));
    }

    @Override
    public SmartTableDO getByUserIdAndDocId(String userId, String docId) {
        return smartTableMapper.selectOne(new LambdaQueryWrapper<SmartTableDO>()
                .eq(SmartTableDO::getUserId, userId)
                .eq(SmartTableDO::getDocId, docId)
                .last("LIMIT 1"));
    }

    @Override
    public List<SmartTableDO> listEnabledMeetingTables() {
        return smartTableMapper.selectList(new LambdaQueryWrapper<SmartTableDO>()
                .eq(SmartTableDO::getTableType, SmartTableTypeEnum.MEETING.getCode())
                .eq(SmartTableDO::getStatus, STATUS_ENABLED));
    }

    @Override
    public void save(SmartTableDO smartTable) {
        smartTableMapper.insert(smartTable);
    }

    @Override
    public void updateById(SmartTableDO smartTable) {
        smartTableMapper.updateById(smartTable);
    }
}
