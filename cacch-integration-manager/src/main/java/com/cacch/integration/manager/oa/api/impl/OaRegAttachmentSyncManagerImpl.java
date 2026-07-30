package com.cacch.integration.manager.oa.api.impl;

import com.cacch.integration.common.config.oa.OaRegAttachmentSyncProperties;
import com.cacch.integration.common.constant.oa.OaRegReportConstants;
import com.cacch.integration.common.constant.sharedrive.ShareDriveConstants;
import com.cacch.integration.common.dto.oa.OaRegAttachmentSyncResult;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.common.enums.oa.OaRegAttachmentSyncStatusEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.oa.OaRegAttachmentSyncDO;
import com.cacch.integration.integration.oa.client.OaRegReportDbClient;
import com.cacch.integration.integration.oa.client.dto.OaRegReportAttachmentBindResult;
import com.cacch.integration.integration.oa.client.dto.OaRegReportItemRow;
import com.cacch.integration.integration.oa.support.OaIdSupport;
import com.cacch.integration.integration.oa.support.OaRegReportItemMatcher;
import com.cacch.integration.integration.oa.support.OaRegReportPathSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import com.cacch.integration.integration.sharedrive.client.IShareDriveClient;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScanRequest;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
import com.cacch.integration.integration.sharedrive.support.ShareDriveFileSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import com.cacch.integration.manager.oa.api.IOaRegAttachmentSyncManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import com.cacch.integration.service.oa.api.IOaRegAttachmentSyncFormMainCursorService;
import com.cacch.integration.service.oa.api.IOaRegAttachmentSyncService;
import com.cacch.integration.service.oa.api.IOaRegReportOpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 国内登记报告附件同步编排实现（共享盘驱动：主表游标分批 + 按负责人动态反查 OA）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OaRegAttachmentSyncManagerImpl implements IOaRegAttachmentSyncManager {

    private static final String BIZ = OaRegReportConstants.LOG_BIZ;

    private final OaRegReportDbClient oaRegReportDbClient;
    private final IShareDriveClient shareDriveClient;
    private final IOaRegReportOpenApiService oaRegReportOpenApiService;
    private final IOaRegAttachmentSyncService syncService;
    private final OaRegAttachmentSyncProperties syncProperties;
    private final IOaRegAttachmentSyncFormMainCursorService formMainCursorService;
    private final IWeComWebhookManager weComWebhookManager;

    @Override
    public OaRegAttachmentSyncResult syncAttachments(Long formMainId) {
        int batchSize = Math.max(1, syncProperties.getBatchSize());
        int formBatchSize = Math.max(1, syncProperties.getFormBatchSize());
        int maxRetry = syncProperties.getMaxRetry() > 0
                ? syncProperties.getMaxRetry()
                : OaRegReportConstants.DEFAULT_MAX_RETRY;
        String ownerFilter = syncProperties.hasOwnerNameFilter()
                ? syncProperties.getOwnerNameFilter()
                : null;

        if (!shareDriveClient.isAvailable()) {
            log.info("【{}】共享盘不可用，本轮同步终止", BIZ);
            return OaRegAttachmentSyncResult.builder()
                    .scanned(0).success(0).retry(0).failed(0).skipped(0).build();
        }

        List<String> cursorBatchFormMainIds = resolveCursorBatchFormMainIds(formMainId, formBatchSize, ownerFilter);
        String ipdpFilter = resolveIpdpFilter(formMainId);
        ShareDriveScanRequest scanRequest = new ShareDriveScanRequest(ownerFilter, ipdpFilter, batchSize);

        Map<String, List<OaRegReportItemRow>> ownerRowsCache = new HashMap<>();
        Map<String, Set<String>> ownerIpdpCollisionCache = new HashMap<>();

        AtomicInteger success = new AtomicInteger();
        AtomicInteger retry = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();

        log.info("【{}】开始附件同步(游标+负责人反查), formMainId={}, cursorBatchForms={}, maxRetry={}, batchSize={}",
                BIZ, formMainId, cursorBatchFormMainIds.size(), maxRetry, batchSize);

        int scanned = shareDriveClient.scanAndProcessItemDirectories(scanRequest, scannedItem -> {
            ShareDriveFile latestFile = scannedItem.latestFile();
            log.info("【{}】识别到含最终版本文件, directoryPath={}, owner={}, ipdp={}, item={}, file={}, size={}, createdAt={}",
                    BIZ, scannedItem.directoryPath(), scannedItem.ownerName(), scannedItem.ipdpName(),
                    scannedItem.itemName(), latestFile != null ? latestFile.fileName() : null,
                    latestFile != null ? latestFile.fileSize() : null,
                    latestFile != null ? latestFile.createdAt() : null);
            try {
                String outcome = syncScannedItem(
                        scannedItem,
                        formMainId,
                        cursorBatchFormMainIds,
                        maxRetry,
                        ownerRowsCache,
                        ownerIpdpCollisionCache);
                if (OaRegAttachmentSyncStatusEnum.SUCCESS.getCode().equals(outcome)) {
                    success.incrementAndGet();
                } else if (OaRegAttachmentSyncStatusEnum.FAILED.getCode().equals(outcome)) {
                    failed.incrementAndGet();
                } else if (OaRegAttachmentSyncStatusEnum.RETRY.getCode().equals(outcome)) {
                    retry.incrementAndGet();
                } else {
                    skipped.incrementAndGet();
                }
            } catch (Exception e) {
                log.info("【{}】单条同步异常, path={}, reason={}", BIZ, scannedItem.directoryPath(), e.getMessage());
                log.error("【{}】单条同步失败, path={}", BIZ, scannedItem.directoryPath(), e);
                failed.incrementAndGet();
            }
        });

        if (scanned == 0) {
            log.info("【{}】共享盘未发现含文件的资料目录, formMainId={}, ownerFilter={}, ipdpFilter={}",
                    BIZ, formMainId, ownerFilter, ipdpFilter);
        }

        OaRegAttachmentSyncResult result = OaRegAttachmentSyncResult.builder()
                .scanned(scanned)
                .success(success.get())
                .retry(retry.get())
                .failed(failed.get())
                .skipped(skipped.get())
                .build();
        log.info("【{}】本轮附件同步完成, scanned={}, success={}, retry={}, failed={}, skipped={}",
                BIZ, result.getScanned(), result.getSuccess(), result.getRetry(),
                result.getFailed(), result.getSkipped());
        return result;
    }

    /**
     * 全量定时同步时推进主表游标；指定 formMainId 时不使用游标
     */
    private List<String> resolveCursorBatchFormMainIds(Long formMainId, int formBatchSize, String ownerFilter) {
        if (formMainId != null && formMainId > 0) {
            return List.of(String.valueOf(formMainId));
        }
        String cursor = formMainCursorService.getLastFormMainId();
        List<String> batch = oaRegReportDbClient.listFormMainIdsAfterCursor(cursor, formBatchSize, ownerFilter);
        if (batch.isEmpty() && StringUtils.hasText(cursor) && !"0".equals(cursor)) {
            log.info("【{}】主表游标已至末尾，从头开始新一轮, previousCursor={}", BIZ, cursor);
            formMainCursorService.resetCursor();
            batch = oaRegReportDbClient.listFormMainIdsAfterCursor("0", formBatchSize, ownerFilter);
        }
        if (!batch.isEmpty()) {
            String newCursor = batch.get(batch.size() - 1);
            formMainCursorService.saveLastFormMainId(newCursor);
            log.info("【{}】本批主表游标批次, previousCursor={}, newCursor={}, formCount={}, formMainIds={}",
                    BIZ, cursor, newCursor, batch.size(), batch);
        } else {
            log.info("【{}】主表游标批次为空, cursor={}", BIZ, cursor);
        }
        return batch;
    }

    private String syncScannedItem(ShareDriveScannedItem scanned,
                                   Long formMainId,
                                   List<String> cursorBatchFormMainIds,
                                   int maxRetry,
                                   Map<String, List<OaRegReportItemRow>> ownerRowsCache,
                                   Map<String, Set<String>> ownerIpdpCollisionCache) {
        ShareDriveFile file = scanned.latestFile();
        if (file == null || !StringUtils.hasText(file.fileName()) || file.fileSize() <= 0) {
            log.info("【{}】跳过同步, reason=扫描结果无有效文件元数据, path={}", BIZ, scanned.directoryPath());
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        OaRegReportItemRow row = resolveOaRow(scanned, formMainId, cursorBatchFormMainIds, ownerRowsCache);
        if (row == null) {
            log.info("【{}】跳过同步, reason=OA未匹配资料行, owner={}, ipdp={}, item={}",
                    BIZ, scanned.ownerName(), scanned.ipdpName(), scanned.itemName());
            OaRegAttachmentSyncDO record = baseRecordFromScan(scanned);
            syncService.markSkipped(record, OaRegReportConstants.SKIP_OA_NOT_FOUND);
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }
        if (looksLikeMemberId(row.ownerName())) {
            log.info("【{}】跳过同步, reason=登记负责人未解析为姓名, subRowId={}", BIZ, row.subRowId());
            OaRegAttachmentSyncDO record = baseRecord(row, scanned.directoryPath());
            syncService.markSkipped(record, OaRegReportConstants.SKIP_OWNER_UNRESOLVED);
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        Set<String> ipdpPathCollisionKeys = ownerIpdpCollisionCache.computeIfAbsent(
                scanned.ownerName(),
                owner -> detectIpdpPathCollisions(loadOwnerRows(owner, formMainId, cursorBatchFormMainIds, ownerRowsCache)));
        String ipdpKey = OaRegReportPathSupport.buildNormalizedIpdpKey(row.ownerName(), row.ipdpName());
        if (ipdpPathCollisionKeys.contains(ipdpKey)) {
            log.info("【{}】跳过同步, reason=IPDP路径冲突, subRowId={}, ipdp={}", BIZ, row.subRowId(), row.ipdpName());
            OaRegAttachmentSyncDO record = baseRecord(row, scanned.directoryPath());
            syncService.markSkipped(record, ShareDriveConstants.SKIP_PATH_COLLISION + ":" + row.ipdpName());
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        OaRegAttachmentSyncDO existing = syncService.findByItemKey(
                row.ownerName(), row.ipdpName(), row.itemName());
        if (syncService.shouldSkipSuccess(existing, file.createdAt())) {
            log.info("【{}】幂等跳过, subRowId={}, item={}, fileCreatedAt={}",
                    BIZ, row.subRowId(), row.itemName(), file.createdAt());
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        String subReference = resolveSubReference(existing, row);
        boolean rotateSubReference = existing != null;
        if (rotateSubReference) {
            log.info("【{}】检测到资料项已有同步记录，将轮换 subReference 绑定新附件, subRowId={}, "
                            + "oldSubReference={}, newCreatedAt={}",
                    BIZ, row.subRowId(), existing.getOaSubReference(), file.createdAt());
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            OaRegReportAttachmentBindResult[] bindHolder = new OaRegReportAttachmentBindResult[1];
            shareDriveClient.readFileStream(scanned, rawStream -> {
                try (DigestInputStream digestStream = new DigestInputStream(rawStream, digest)) {
                    bindHolder[0] = oaRegReportOpenApiService.replaceAttachment(
                            digestStream,
                            file.fileSize(),
                            file.fileName(),
                            file.contentType(),
                            row.formMainId(),
                            row.subRowId(),
                            subReference,
                            rotateSubReference,
                            null,
                            null,
                            null,
                            1,
                            null);
                }
            });
            String checksum = ShareDriveFileSupport.sha256Hex(digest);
            OaRegReportAttachmentBindResult bindResult = bindHolder[0];
            if (bindResult == null) {
                log.info("【{}】同步终止, reason=流式上传未返回绑定结果, subRowId={}", BIZ, row.subRowId());
                throw new BizException(com.cacch.integration.common.result.ResultCode.INTEGRATION_ERROR,
                        "流式上传未返回绑定结果");
            }

            OaRegAttachmentSyncDO record = enrichRecord(baseRecord(row, scanned.directoryPath()), file, checksum);
            record.setOaFileId(bindResult.fileUrl());
            record.setOaSubReference(bindResult.subReference());
            syncService.markSuccess(record);
            log.info("【{}】同步成功, formMainId={}, subRowId={}, item={}, fileUrl={}, fileSize={}",
                    BIZ, row.formMainId(), row.subRowId(), row.itemName(), bindResult.fileUrl(), file.fileSize());
            return OaRegAttachmentSyncStatusEnum.SUCCESS.getCode();
        } catch (IOException e) {
            log.info("【{}】读取共享盘文件失败, subRowId={}, item={}, reason={}",
                    BIZ, row.subRowId(), row.itemName(), e.getMessage());
            OaRegAttachmentSyncDO record = enrichRecord(baseRecord(row, scanned.directoryPath()), file, null);
            String status = syncService.markFailure(record, "读取共享盘文件失败: " + e.getMessage(), maxRetry);
            if (OaRegAttachmentSyncStatusEnum.FAILED.getCode().equals(status)) {
                alertFailed(row, e.getMessage());
            }
            return status;
        } catch (NoSuchAlgorithmException e) {
            log.info("【{}】计算文件摘要失败, subRowId={}, reason={}", BIZ, row.subRowId(), e.getMessage());
            OaRegAttachmentSyncDO record = enrichRecord(baseRecord(row, scanned.directoryPath()), file, null);
            return syncService.markFailure(record, "计算文件摘要失败: " + e.getMessage(), maxRetry);
        } catch (BizException e) {
            log.info("【{}】同步业务失败, subRowId={}, item={}, reason={}",
                    BIZ, row.subRowId(), row.itemName(), e.getMessage());
            OaRegAttachmentSyncDO record = enrichRecord(baseRecord(row, scanned.directoryPath()), file, null);
            String status = syncService.markFailure(record, e.getMessage(), maxRetry);
            if (OaRegAttachmentSyncStatusEnum.FAILED.getCode().equals(status)) {
                alertFailed(row, e.getMessage());
            }
            return status;
        }
    }

    private OaRegReportItemRow resolveOaRow(ShareDriveScannedItem scanned,
                                          Long formMainId,
                                          List<String> cursorBatchFormMainIds,
                                          Map<String, List<OaRegReportItemRow>> ownerRowsCache) {
        List<OaRegReportItemRow> ownerRows = loadOwnerRows(
                scanned.ownerName(), formMainId, cursorBatchFormMainIds, ownerRowsCache);
        String formMainIdFilter = formMainId != null && formMainId > 0 ? String.valueOf(formMainId) : null;
        OaRegReportItemRow row = OaRegReportItemMatcher.match(
                ownerRows, scanned, formMainIdFilter, cursorBatchFormMainIds);
        if (row == null && !ownerRows.isEmpty()) {
            List<String> itemMatchedSamples = ownerRows.stream()
                    .filter(r -> ShareDrivePathNormalizer.matchesDirectoryNameLoosely(
                            r.itemName(), scanned.itemName()))
                    .limit(5)
                    .map(r -> "formMainId=" + r.formMainId()
                            + ", owner=" + r.ownerName()
                            + ", ipdp=" + r.ipdpName()
                            + ", item=" + r.itemName())
                    .toList();
            log.info("【{}】负责人资料行已加载但未匹配路径, diskOwner={}, diskIpdp={}, diskItem={}, "
                            + "candidateCount={}, itemMatchedSamples={}, ambiguous={}",
                    BIZ, scanned.ownerName(), scanned.ipdpName(), scanned.itemName(), ownerRows.size(),
                    itemMatchedSamples,
                    OaRegReportItemMatcher.hasAmbiguousMatch(ownerRows, scanned, formMainIdFilter));
        }
        if (row != null) {
            return row;
        }
        return oaRegReportDbClient.findItemRowByDirectory(scanned, formMainId, cursorBatchFormMainIds);
    }

    private List<OaRegReportItemRow> loadOwnerRows(String diskOwnerName,
                                                   Long formMainId,
                                                   List<String> cursorBatchFormMainIds,
                                                   Map<String, List<OaRegReportItemRow>> ownerRowsCache) {
        if (!StringUtils.hasText(diskOwnerName)) {
            return List.of();
        }
        String cacheKey = diskOwnerName.trim();
        List<OaRegReportItemRow> cached = ownerRowsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String formMainIdFilter = formMainId != null && formMainId > 0 ? String.valueOf(formMainId) : null;
        List<OaRegReportItemRow> rows = oaRegReportDbClient.listItemRowsByOwnerName(cacheKey, formMainIdFilter);
        if (rows.isEmpty() && formMainIdFilter == null && !cursorBatchFormMainIds.isEmpty()) {
            rows = oaRegReportDbClient.listItemRowsByFormMainIds(cursorBatchFormMainIds).stream()
                    .filter(row -> ShareDrivePathNormalizer.matchesDirectoryNameLoosely(cacheKey, row.ownerName()))
                    .toList();
            if (!rows.isEmpty()) {
                log.info("【{}】负责人精确反查无结果，游标批次宽松匹配命中, diskOwner={}, rowCount={}",
                        BIZ, cacheKey, rows.size());
            }
        }
        ownerRowsCache.put(cacheKey, rows);
        log.info("【{}】负责人资料行已加载, diskOwner={}, rowCount={}", BIZ, cacheKey, rows.size());
        return rows;
    }

    private String resolveIpdpFilter(Long formMainId) {
        if (formMainId == null || formMainId <= 0) {
            return null;
        }
        return oaRegReportDbClient.listItemRowsByFormMainIds(List.of(String.valueOf(formMainId))).stream()
                .map(OaRegReportItemRow::ipdpName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private Set<String> detectIpdpPathCollisions(List<OaRegReportItemRow> rows) {
        Map<String, String> normalizedToOaIpdp = new HashMap<>();
        Set<String> collisions = new HashSet<>();
        for (OaRegReportItemRow row : rows) {
            if (!StringUtils.hasText(row.ownerName()) || !StringUtils.hasText(row.ipdpName())) {
                continue;
            }
            String key = OaRegReportPathSupport.buildNormalizedIpdpKey(row.ownerName(), row.ipdpName());
            if (!StringUtils.hasText(key) || key.equals("|")) {
                continue;
            }
            String previous = normalizedToOaIpdp.put(key, row.ipdpName());
            if (previous != null && !previous.equals(row.ipdpName())) {
                collisions.add(key);
            }
        }
        return collisions;
    }

    private static String resolveSubReference(OaRegAttachmentSyncDO existing, OaRegReportItemRow row) {
        if (existing != null && StringUtils.hasText(existing.getOaSubReference())) {
            return existing.getOaSubReference();
        }
        if (StringUtils.hasText(row.currentAttachmentRef())) {
            return row.currentAttachmentRef();
        }
        return null;
    }

    private static boolean looksLikeMemberId(String ownerName) {
        return StringUtils.hasText(ownerName) && ownerName.trim().matches("-?\\d+");
    }

    private OaRegAttachmentSyncDO baseRecordFromScan(ShareDriveScannedItem scanned) {
        OaRegAttachmentSyncDO record = new OaRegAttachmentSyncDO();
        record.setOwnerName(scanned.ownerName());
        record.setIpdpName(scanned.ipdpName());
        record.setItemName(scanned.itemName());
        record.setSharePath(scanned.directoryPath());
        record.setRetryCount(0);
        ShareDriveFile file = scanned.latestFile();
        if (file != null) {
            enrichRecord(record, file, null);
        }
        return record;
    }

    private OaRegAttachmentSyncDO baseRecord(OaRegReportItemRow row, String sharePath) {
        OaRegAttachmentSyncDO record = new OaRegAttachmentSyncDO();
        record.setFormMainId(OaIdSupport.toStorageLong(row.formMainId()));
        record.setOwnerName(row.ownerName());
        record.setIpdpName(row.ipdpName());
        record.setItemName(row.itemName());
        record.setItemRowId(OaIdSupport.toStorageLong(row.subRowId()));
        record.setSharePath(sharePath);
        record.setRetryCount(0);
        return record;
    }

    private OaRegAttachmentSyncDO enrichRecord(OaRegAttachmentSyncDO record, ShareDriveFile file, String checksum) {
        record.setFileName(file.fileName());
        record.setFileSize(file.fileSize());
        record.setFileChecksum(checksum);
        record.setFileCreatedAt(file.createdAt());
        record.setFileModifiedAt(file.modifiedAt());
        return record;
    }

    private void alertFailed(OaRegReportItemRow row, String errorMessage) {
        weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                .biz("oa")
                .title("国内登记报告附件同步失败需人工介入")
                .subject(row.itemName())
                .context("formMainId=" + row.formMainId() + ", subRowId=" + row.subRowId()
                        + ", owner=" + row.ownerName())
                .errorMessage(errorMessage)
                .dedupType("oa-reg-attachment-sync")
                .dedupId(String.valueOf(row.subRowId()))
                .mention(true)
                .build());
    }
}
