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

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public boolean claimForProcess(Long id, Integer expectedVersion) {
        if (id == null || expectedVersion == null) {
            log.info("【{}】抢占跳过, reason=参数为空, id={}, version={}", BIZ, id, expectedVersion);
            return false;
        }
        int claimed = syncMapper.claimForProcess(id, expectedVersion);
        if (claimed == 0) {
            log.info("【{}】抢占失败跳过处理, id={}, version={}, reason=并发冲突或状态已变化", BIZ, id, expectedVersion);
        }
        return claimed > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public boolean markAttachmentReady(Long id,
                                       Integer expectedVersion,
                                       String oaAttachmentId,
                                       String attachmentFileId,
                                       String attachmentFileName,
                                       String attachmentUrl) {
        int updated = syncMapper.markAttachmentReady(id, expectedVersion,
                oaAttachmentId, attachmentFileId, attachmentFileName, attachmentUrl);
        if (updated == 0) {
            log.info("【{}】附件就绪回填未生效, id={}, version={}, reason=状态或版本已变化", BIZ, id, expectedVersion);
        }
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public boolean markSkipped(Long id, Integer expectedVersion, String skipReason) {
        int updated = syncMapper.markSkipped(id, expectedVersion, skipReason);
        if (updated == 0) {
            log.info("【{}】跳过回写未生效, id={}, version={}, skipReason={}",
                    BIZ, id, expectedVersion, skipReason);
        }
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public boolean markRetry(Long id, Integer expectedVersion, String syncMessage) {
        String message = truncateMessage(syncMessage);
        int updated = syncMapper.markRetry(id, expectedVersion, message);
        if (updated == 0) {
            log.info("【{}】失败回写未生效, id={}, version={}, reason=状态或版本已变化", BIZ, id, expectedVersion);
        }
        return updated > 0;
    }

    /**
     * 失败原因截断至 {@code MESSAGE_MAX_LENGTH}
     *
     * @param message 原始失败原因，可空
     * @return 截断后的失败原因；入参为空时返回 null
     */
    private static String truncateMessage(String message) {
        if (message == null || message.length() <= OaContractAttachmentConstants.MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, OaContractAttachmentConstants.MESSAGE_MAX_LENGTH);
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
