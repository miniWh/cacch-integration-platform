package com.cacch.integration.service.mdm.api.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.entity.mdm.StgStaffRecordDO;
import com.cacch.integration.mapper.mdm.StgStaffRecordMapper;
import com.cacch.integration.service.mdm.api.IMdmEmpIncrementalQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MDM 增量员工暂存队列 Service 实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmEmpIncrementalQueueServiceImpl implements IMdmEmpIncrementalQueueService {

    private final StgStaffRecordMapper stgStaffRecordMapper;

    @Override
    public int enqueueOne(String staffId, String source, Long lastUpdate, String pullBatchNo) {
        long id = IdWorker.getId();
        return stgStaffRecordMapper.insertIgnore(id, staffId, source, lastUpdate, pullBatchNo);
    }

    @Override
    public List<StgStaffRecordDO> pollPendingBatch(int limit) {
        return stgStaffRecordMapper.selectPendingBatch(limit);
    }

    @Override
    public void markOneProcessed(Long id, String processBatchNo) {
        stgStaffRecordMapper.markProcessed(id, processBatchNo);
    }
}
