package com.cacch.integration.service.oa.api.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.constant.oa.OaContractAttachmentConstants;
import com.cacch.integration.common.enums.oa.OaContractAttachmentSyncStatusEnum;
import com.cacch.integration.entity.oa.OaContractAttachmentSyncDO;
import com.cacch.integration.integration.oa.client.dto.OaContractRow;
import com.cacch.integration.mapper.oa.OaContractAttachmentSyncMapper;
import com.cacch.integration.service.oa.api.IOaContractAttachmentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OA 货物购销合同附件回传 CRM 中间记录服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaContractAttachmentSyncServiceImpl implements IOaContractAttachmentSyncService {

    private static final String BIZ = OaContractAttachmentConstants.LOG_BIZ;

    private final OaContractAttachmentSyncMapper syncMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10)
    public List<Long> listExistingFormMainIds() {
        return syncMapper.selectExistingFormMainIds();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public int upsertFromScan(List<OaContractRow> rows) {
        if (rows == null || rows.isEmpty()) {
            log.info("【{}】扫描结果为空, 跳过入库", BIZ);
            return 0;
        }
        // 按 oa_form_main_id 去重（保留首条）；无主键的脏数据丢弃
        Map<Long, OaContractRow> distinct = new LinkedHashMap<>();
        int invalid = 0;
        for (OaContractRow row : rows) {
            if (row == null || row.oaFormMainId() == null) {
                invalid++;
                continue;
            }
            distinct.putIfAbsent(row.oaFormMainId(), row);
        }
        if (invalid > 0) {
            log.warn("【{}】扫描结果含无主键脏数据, invalid={}", BIZ, invalid);
        }
        if (distinct.isEmpty()) {
            log.info("【{}】扫描结果去重后为空, 跳过入库, rows={}", BIZ, rows.size());
            return 0;
        }

        List<OaContractAttachmentSyncDO> records = distinct.values().stream()
                .map(OaContractAttachmentSyncServiceImpl::toDO)
                .toList();
        int affected = syncMapper.upsertBatch(records);
        log.info("【{}】扫描结果入库完成, rows={}, 去重后={}, 受影响行数={}",
                BIZ, rows.size(), distinct.size(), affected);
        return distinct.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10)
    public List<OaContractAttachmentSyncDO> listPendingForProcess(int limit) {
        if (limit <= 0) {
            log.info("【{}】捞取待处理跳过, reason=捞取条数非法, limit={}", BIZ, limit);
            return List.of();
        }
        return syncMapper.selectPendingForProcess(limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public int reclaimTimeoutProcessing(int timeoutMinutes) {
        if (timeoutMinutes <= 0) {
            log.info("【{}】PROCESSING 超时回收跳过, reason=回收阈值非法, timeoutMinutes={}", BIZ, timeoutMinutes);
            return 0;
        }
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        int reclaimed = syncMapper.reclaimTimeoutProcessing(threshold);
        log.info("【{}】PROCESSING 超时回收完成, threshold={}, reclaimed={}", BIZ, threshold, reclaimed);
        return reclaimed;
    }

    /**
     * 扫描行转中间记录 DO（主键预生成，初始状态 PENDING）
     *
     * @param row 扫描行
     * @return 中间记录 DO
     */
    private static OaContractAttachmentSyncDO toDO(OaContractRow row) {
        OaContractAttachmentSyncDO record = new OaContractAttachmentSyncDO();
        record.setId(IdWorker.getId());
        record.setOaFormMainId(row.oaFormMainId());
        record.setOaStartDate(row.oaStartDate());
        record.setFlowStatus(row.flowStatus());
        record.setNcOrderNo(row.ncOrderNo());
        record.setOaContractNo(row.oaContractNo());
        record.setOaAttachmentRef(row.oaAttachmentRef());
        record.setSyncStatus(OaContractAttachmentSyncStatusEnum.PENDING.getCode());
        record.setRetryCount(0);
        record.setVersion(0);
        record.setIsDeleted(0);
        return record;
    }
}
