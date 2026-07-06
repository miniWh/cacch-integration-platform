package com.cacch.integration.service.meeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cacch.integration.common.constant.meeting.MeetingConstants;
import com.cacch.integration.common.enums.meeting.SmartTableTypeEnum;
import com.cacch.integration.entity.meeting.SmartTableDO;
import com.cacch.integration.mapper.meeting.SmartTableMapper;
import com.cacch.integration.service.meeting.api.ISmartTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能表格配置服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Service
@RequiredArgsConstructor
public class SmartTableServiceImpl implements ISmartTableService {

    private final SmartTableMapper smartTableMapper;

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public SmartTableDO getById(Long id) {
        return smartTableMapper.selectById(id);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public SmartTableDO getEnabledMaster() {
        return smartTableMapper.selectOne(new LambdaQueryWrapper<SmartTableDO>()
                .eq(SmartTableDO::getTableType, SmartTableTypeEnum.MASTER.getCode())
                .eq(SmartTableDO::getStatus, MeetingConstants.SMART_TABLE_STATUS_ENABLED)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public SmartTableDO getByUserIdAndDocId(String userId, String docId) {
        return smartTableMapper.selectOne(new LambdaQueryWrapper<SmartTableDO>()
                .eq(SmartTableDO::getUserId, userId)
                .eq(SmartTableDO::getDocId, docId)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public SmartTableDO getEnabledMeetingByUserId(String userId) {
        return smartTableMapper.selectOne(new LambdaQueryWrapper<SmartTableDO>()
                .eq(SmartTableDO::getTableType, SmartTableTypeEnum.MEETING.getCode())
                .eq(SmartTableDO::getUserId, userId)
                .eq(SmartTableDO::getStatus, MeetingConstants.SMART_TABLE_STATUS_ENABLED)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public List<SmartTableDO> listEnabledMeetingTables() {
        return smartTableMapper.selectList(new LambdaQueryWrapper<SmartTableDO>()
                .eq(SmartTableDO::getTableType, SmartTableTypeEnum.MEETING.getCode())
                .eq(SmartTableDO::getStatus, MeetingConstants.SMART_TABLE_STATUS_ENABLED));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public SmartTableDO saveNew(SmartTableDO smartTable) {
        if (smartTable.getStatus() == null) {
            smartTable.setStatus(MeetingConstants.SMART_TABLE_STATUS_ENABLED);
        }
        smartTableMapper.insert(smartTable);
        return smartTable;
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void updateById(SmartTableDO smartTable) {
        smartTableMapper.updateById(smartTable);
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void markSyncSuccess(Long smartTableId) {
        LocalDateTime now = LocalDateTime.now();
        smartTableMapper.update(null, new LambdaUpdateWrapper<SmartTableDO>()
                .eq(SmartTableDO::getId, smartTableId)
                .set(SmartTableDO::getLastSyncTime, now)
                .set(SmartTableDO::getLastSyncError, null)
                .set(SmartTableDO::getUpdatedAt, now));
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRED,
            readOnly = false,
            timeout = 30
    )
    public void markSyncError(Long smartTableId, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        smartTableMapper.update(null, new LambdaUpdateWrapper<SmartTableDO>()
                .eq(SmartTableDO::getId, smartTableId)
                .set(SmartTableDO::getLastSyncTime, now)
                .set(SmartTableDO::getLastSyncError, errorMessage)
                .set(SmartTableDO::getUpdatedAt, now));
    }
}
