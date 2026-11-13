package com.cacch.integration.service.mdm.api.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.entity.mdm.StgStaffRecordDO;
import com.cacch.integration.mapper.mdm.StgStaffRecordMapper;
import com.cacch.integration.service.mdm.api.IMdmEmpIncrementalQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;

    private final StgStaffRecordMapper stgStaffRecordMapper;

    @Override
    public int enqueueOne(String staffId, String source, Long lastUpdate, String pullBatchNo) {
        long id = IdWorker.getId();
        return stgStaffRecordMapper.insertIgnore(id, staffId, source, lastUpdate, pullBatchNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public List<StgStaffRecordDO> claimPendingBatch(int limit) {
        int reset = stgStaffRecordMapper.resetStaleProcessing();
        if (reset > 0) {
            log.info("【{}】回收超时 PROCESSING 行重置为 PENDING, rows={}", BIZ, reset);
        }
        return stgStaffRecordMapper.claimPendingBatch(limit);
    }

    @Override
    public void markOneProcessed(Long id, String processBatchNo) {
        stgStaffRecordMapper.markProcessed(id, processBatchNo);
    }
}
