package com.cacch.integration.service.oa.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cacch.integration.common.constant.oa.OaRegReportConstants;
import com.cacch.integration.common.enums.oa.OaRegAttachmentSyncStatusEnum;
import com.cacch.integration.entity.oa.OaRegAttachmentSyncDO;
import com.cacch.integration.mapper.oa.OaRegAttachmentSyncMapper;
import com.cacch.integration.service.oa.api.IOaRegAttachmentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 国内登记报告附件同步记录服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaRegAttachmentSyncServiceImpl implements IOaRegAttachmentSyncService {

    private static final String BIZ = OaRegReportConstants.LOG_BIZ;

    private final OaRegAttachmentSyncMapper syncMapper;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public OaRegAttachmentSyncDO findByBizKey(String ownerName,
                                              String ipdpName,
                                              String itemName,
                                              Integer fileVersion) {
        if (!StringUtils.hasText(ownerName) || !StringUtils.hasText(ipdpName) || !StringUtils.hasText(itemName)
                || fileVersion == null) {
            return null;
        }
        return syncMapper.selectOne(new LambdaQueryWrapper<OaRegAttachmentSyncDO>()
                .eq(OaRegAttachmentSyncDO::getOwnerName, ownerName.trim())
                .eq(OaRegAttachmentSyncDO::getIpdpName, ipdpName.trim())
                .eq(OaRegAttachmentSyncDO::getItemName, itemName.trim())
                .eq(OaRegAttachmentSyncDO::getFileVersion, fileVersion)
                .last("LIMIT 1"));
    }

    @Override
    public boolean shouldSkipSuccess(OaRegAttachmentSyncDO existing, String checksum) {
        if (existing == null) {
            return false;
        }
        if (!OaRegAttachmentSyncStatusEnum.SUCCESS.getCode().equals(existing.getSyncStatus())) {
            return false;
        }
        if (!StringUtils.hasText(checksum)) {
            return true;
        }
        return Objects.equals(checksum, existing.getFileChecksum());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public void markSuccess(OaRegAttachmentSyncDO record) {
        if (record == null) {
            log.info("【{}】成功回写终止, reason=record为空", BIZ);
            return;
        }
        if (record.getFileVersion() == null) {
            log.info("【{}】成功回写终止, reason=fileVersion为空, itemRowId={}", BIZ, record.getItemRowId());
            return;
        }
        OaRegAttachmentSyncDO existing = findByBizKey(record.getOwnerName(), record.getIpdpName(),
                record.getItemName(), record.getFileVersion());
        LocalDateTime now = LocalDateTime.now();
        record.setSyncStatus(OaRegAttachmentSyncStatusEnum.SUCCESS.getCode());
        record.setRetryCount(0);
        record.setSyncMessage(null);
        record.setLastSyncAt(now);
        if (existing == null) {
            if (record.getRetryCount() == null) {
                record.setRetryCount(0);
            }
            syncMapper.insert(record);
            log.info("【{}】新增成功记录, owner={}, ipdp={}, item={}, version={}",
                    BIZ, record.getOwnerName(), record.getIpdpName(), record.getItemName(), record.getFileVersion());
            return;
        }
        record.setId(existing.getId());
        syncMapper.updateById(record);
        log.info("【{}】更新成功记录, id={}, owner={}, item={}, version={}",
                BIZ, existing.getId(), record.getOwnerName(), record.getItemName(), record.getFileVersion());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public String markFailure(OaRegAttachmentSyncDO record, String errorMsg, int maxRetry) {
        if (record == null) {
            log.info("【{}】失败回写终止, reason=record为空", BIZ);
            return OaRegAttachmentSyncStatusEnum.RETRY.getCode();
        }
        OaRegAttachmentSyncDO existing = resolveExistingForUpdate(record);
        int currentRetry = existing != null && existing.getRetryCount() != null ? existing.getRetryCount() : 0;
        int nextRetry = currentRetry + 1;
        int resolvedMaxRetry = maxRetry > 0 ? maxRetry : OaRegReportConstants.DEFAULT_MAX_RETRY;
        String status = nextRetry >= resolvedMaxRetry
                ? OaRegAttachmentSyncStatusEnum.FAILED.getCode()
                : OaRegAttachmentSyncStatusEnum.RETRY.getCode();
        record.setSyncStatus(status);
        record.setRetryCount(nextRetry);
        record.setSyncMessage(truncate(errorMsg, 2000));
        record.setLastSyncAt(LocalDateTime.now());
        if (existing == null) {
            syncMapper.insert(record);
        } else {
            record.setId(existing.getId());
            syncMapper.updateById(record);
        }
        log.info("【{}】失败回写, owner={}, item={}, version={}, itemRowId={}, status={}, retryCount={}",
                BIZ, record.getOwnerName(), record.getItemName(), record.getFileVersion(), record.getItemRowId(),
                status, nextRetry);
        return status;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false, timeout = 30, rollbackFor = Exception.class)
    public void markSkipped(OaRegAttachmentSyncDO record, String message) {
        if (record == null) {
            log.info("【{}】跳过回写终止, reason=record为空", BIZ);
            return;
        }
        OaRegAttachmentSyncDO existing = record.getFileVersion() != null
                ? findByBizKey(record.getOwnerName(), record.getIpdpName(), record.getItemName(),
                record.getFileVersion())
                : findLatestSkippedByItemRow(record.getItemRowId());
        record.setSyncStatus(OaRegAttachmentSyncStatusEnum.SKIPPED.getCode());
        record.setSyncMessage(truncate(message, 2000));
        record.setLastSyncAt(LocalDateTime.now());
        if (record.getRetryCount() == null) {
            record.setRetryCount(0);
        }
        if (existing == null) {
            syncMapper.insert(record);
        } else {
            record.setId(existing.getId());
            syncMapper.updateById(record);
        }
        log.info("【{}】跳过回写, owner={}, item={}, message={}",
                BIZ, record.getOwnerName(), record.getItemName(), message);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public IPage<OaRegAttachmentSyncDO> pageByFormMainId(Long formMainId, long page, long size) {
        long resolvedPage = page > 0 ? page : 1;
        long resolvedSize = size > 0 ? size : 20;
        LambdaQueryWrapper<OaRegAttachmentSyncDO> wrapper = new LambdaQueryWrapper<OaRegAttachmentSyncDO>()
                .orderByDesc(OaRegAttachmentSyncDO::getLastSyncAt)
                .orderByDesc(OaRegAttachmentSyncDO::getId);
        if (formMainId != null && formMainId > 0) {
            wrapper.eq(OaRegAttachmentSyncDO::getFormMainId, formMainId);
        }
        return syncMapper.selectPage(new Page<>(resolvedPage, resolvedSize), wrapper);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10, rollbackFor = Exception.class)
    public IPage<OaRegAttachmentSyncDO> pageQuery(String syncStatus, long page, long size) {
        long resolvedPage = page > 0 ? page : 1;
        long resolvedSize = size > 0 ? size : 20;
        LambdaQueryWrapper<OaRegAttachmentSyncDO> wrapper = new LambdaQueryWrapper<OaRegAttachmentSyncDO>()
                .orderByDesc(OaRegAttachmentSyncDO::getLastSyncAt)
                .orderByDesc(OaRegAttachmentSyncDO::getId);
        if (StringUtils.hasText(syncStatus)) {
            wrapper.eq(OaRegAttachmentSyncDO::getSyncStatus, syncStatus.trim());
        }
        return syncMapper.selectPage(new Page<>(resolvedPage, resolvedSize), wrapper);
    }

    /**
     * 解析待更新的失败/重试记录（有版本走幂等键，无版本按子表行 ID）
     *
     * @param record 待写入记录
     * @return 已有记录；不存在时返回 null
     */
    private OaRegAttachmentSyncDO resolveExistingForUpdate(OaRegAttachmentSyncDO record) {
        if (record.getFileVersion() != null) {
            return findByBizKey(record.getOwnerName(), record.getIpdpName(), record.getItemName(),
                    record.getFileVersion());
        }
        return findLatestRetryableByItemRow(record.getItemRowId());
    }

    /**
     * 按子表行 ID 查找最近一条可重试记录（fileVersion 为空或未 SUCCESS 的场景）
     *
     * @param itemRowId 子表行 ID
     * @return 记录；不存在时返回 null
     */
    private OaRegAttachmentSyncDO findLatestRetryableByItemRow(Long itemRowId) {
        if (itemRowId == null) {
            return null;
        }
        List<String> retryableStatuses = List.of(
                OaRegAttachmentSyncStatusEnum.PENDING.getCode(),
                OaRegAttachmentSyncStatusEnum.RETRY.getCode(),
                OaRegAttachmentSyncStatusEnum.FAILED.getCode());
        return syncMapper.selectOne(new LambdaQueryWrapper<OaRegAttachmentSyncDO>()
                .eq(OaRegAttachmentSyncDO::getItemRowId, itemRowId)
                .in(OaRegAttachmentSyncDO::getSyncStatus, retryableStatuses)
                .orderByDesc(OaRegAttachmentSyncDO::getLastSyncAt)
                .orderByDesc(OaRegAttachmentSyncDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 按子表行 ID 查找最近一条 SKIPPED 记录（无版本号跳过场景去重）
     *
     * @param itemRowId 子表行 ID
     * @return 记录；不存在时返回 null
     */
    private OaRegAttachmentSyncDO findLatestSkippedByItemRow(Long itemRowId) {
        if (itemRowId == null) {
            return null;
        }
        return syncMapper.selectOne(new LambdaQueryWrapper<OaRegAttachmentSyncDO>()
                .eq(OaRegAttachmentSyncDO::getItemRowId, itemRowId)
                .eq(OaRegAttachmentSyncDO::getSyncStatus, OaRegAttachmentSyncStatusEnum.SKIPPED.getCode())
                .isNull(OaRegAttachmentSyncDO::getFileVersion)
                .orderByDesc(OaRegAttachmentSyncDO::getLastSyncAt)
                .last("LIMIT 1"));
    }

    private static String truncate(String text, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }
}
