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
import com.cacch.integration.integration.oa.support.OaRegReportItemMatcher;
import com.cacch.integration.integration.oa.support.OaRegReportPathSupport;
import com.cacch.integration.integration.sharedrive.client.IShareDriveClient;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScanRequest;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
import com.cacch.integration.manager.oa.api.IOaRegAttachmentSyncManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import com.cacch.integration.service.oa.api.IOaRegAttachmentSyncService;
import com.cacch.integration.service.oa.api.IOaRegReportOpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 国内登记报告附件同步编排实现（共享盘驱动：先扫盘再反查 OA）
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

        List<OaRegReportItemRow> oaLookupRows = oaRegReportDbClient.listRegReportItemsForLookup(
                formMainId, formBatchSize, ownerFilter);
        String ipdpFilter = resolveIpdpFilter(formMainId, oaLookupRows);
        Set<String> ipdpPathCollisionKeys = detectIpdpPathCollisions(oaLookupRows);

        ShareDriveScanRequest scanRequest = new ShareDriveScanRequest(ownerFilter, ipdpFilter, batchSize);
        List<ShareDriveScannedItem> scannedItems = shareDriveClient.scanItemDirectories(scanRequest);
        if (scannedItems.isEmpty()) {
            log.info("【{}】共享盘未发现含文件的资料目录, formMainId={}, ownerFilter={}, ipdpFilter={}",
                    BIZ, formMainId, ownerFilter, ipdpFilter);
            return OaRegAttachmentSyncResult.builder()
                    .scanned(0).success(0).retry(0).failed(0).skipped(0).build();
        }

        log.info("【{}】开始附件同步(共享盘驱动), formMainId={}, scannedDirs={}, oaLookupRows={}, maxRetry={}",
                BIZ, formMainId, scannedItems.size(), oaLookupRows.size(), maxRetry);

        int success = 0;
        int retry = 0;
        int failed = 0;
        int skipped = 0;

        for (ShareDriveScannedItem scanned : scannedItems) {
            try {
                String outcome = syncScannedItem(scanned, formMainId, oaLookupRows, maxRetry, ipdpPathCollisionKeys);
                if (OaRegAttachmentSyncStatusEnum.SUCCESS.getCode().equals(outcome)) {
                    success++;
                } else if (OaRegAttachmentSyncStatusEnum.FAILED.getCode().equals(outcome)) {
                    failed++;
                } else if (OaRegAttachmentSyncStatusEnum.RETRY.getCode().equals(outcome)) {
                    retry++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                log.info("【{}】单条同步异常, path={}, reason={}", BIZ, scanned.directoryPath(), e.getMessage());
                log.error("【{}】单条同步失败, path={}", BIZ, scanned.directoryPath(), e);
                failed++;
            }
        }

        OaRegAttachmentSyncResult result = OaRegAttachmentSyncResult.builder()
                .scanned(scannedItems.size())
                .success(success)
                .retry(retry)
                .failed(failed)
                .skipped(skipped)
                .build();
        log.info("【{}】本轮附件同步完成, scanned={}, success={}, retry={}, failed={}, skipped={}",
                BIZ, result.getScanned(), result.getSuccess(), result.getRetry(),
                result.getFailed(), result.getSkipped());
        return result;
    }

    private String syncScannedItem(ShareDriveScannedItem scanned,
                                   Long formMainId,
                                   List<OaRegReportItemRow> oaLookupRows,
                                   int maxRetry,
                                   Set<String> ipdpPathCollisionKeys) {
        ShareDriveFile file = scanned.latestFile();
        if (file == null || file.content() == null || file.content().length == 0) {
            log.info("【{}】跳过同步, reason=扫描结果无有效文件, path={}", BIZ, scanned.directoryPath());
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        OaRegReportItemRow row = OaRegReportItemMatcher.match(oaLookupRows, scanned, formMainId);
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

        String ipdpKey = OaRegReportPathSupport.buildNormalizedIpdpKey(row.ownerName(), row.ipdpName());
        if (ipdpPathCollisionKeys.contains(ipdpKey)) {
            log.info("【{}】跳过同步, reason=IPDP路径冲突, subRowId={}, ipdp={}", BIZ, row.subRowId(), row.ipdpName());
            OaRegAttachmentSyncDO record = baseRecord(row, scanned.directoryPath());
            syncService.markSkipped(record, ShareDriveConstants.SKIP_PATH_COLLISION + ":" + row.ipdpName());
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        OaRegAttachmentSyncDO existing = syncService.findByBizKey(
                row.ownerName(), row.ipdpName(), row.itemName(), file.fileVersion());
        if (syncService.shouldSkipSuccess(existing, file.checksum())) {
            log.info("【{}】幂等跳过, subRowId={}, item={}, version={}",
                    BIZ, row.subRowId(), row.itemName(), file.fileVersion());
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        /*try {
            OaRegReportAttachmentBindResult bindResult = oaRegReportOpenApiService.uploadAndBindAttachment(
                    file.content(),
                    file.fileName(),
                    file.contentType(),
                    row.formMainId(),
                    row.subRowId(),
                    row.currentAttachmentRef(),
                    null,
                    null,
                    null,
                    1,
                    null);

            OaRegAttachmentSyncDO record = enrichRecord(baseRecord(row, scanned.directoryPath()), file);
            record.setOaFileId(bindResult.fileUrl());
            record.setOaSubReference(bindResult.subReference());
            syncService.markSuccess(record);
            log.info("【{}】同步成功, formMainId={}, subRowId={}, item={}, fileUrl={}",
                    BIZ, row.formMainId(), row.subRowId(), row.itemName(), bindResult.fileUrl());
            return OaRegAttachmentSyncStatusEnum.SUCCESS.getCode();
        } catch (BizException e) {
            log.info("【{}】同步业务失败, subRowId={}, item={}, reason={}",
                    BIZ, row.subRowId(), row.itemName(), e.getMessage());
            OaRegAttachmentSyncDO record = enrichRecord(baseRecord(row, scanned.directoryPath()), file);
            String status = syncService.markFailure(record, e.getMessage(), maxRetry);
            if (OaRegAttachmentSyncStatusEnum.FAILED.getCode().equals(status)) {
                alertFailed(row, e.getMessage());
            }
            return status;
        }*/
        log.info("【{}】待上传绑定(联调占位), subRowId={}, item={}, file={}",
                BIZ, row.subRowId(), row.itemName(), file.fileName());
        return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
    }

    private static String resolveIpdpFilter(Long formMainId, List<OaRegReportItemRow> oaLookupRows) {
        if (formMainId == null || formMainId <= 0 || oaLookupRows.isEmpty()) {
            return null;
        }
        return oaLookupRows.stream()
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
            enrichRecord(record, file);
        }
        return record;
    }

    private OaRegAttachmentSyncDO baseRecord(OaRegReportItemRow row, String sharePath) {
        OaRegAttachmentSyncDO record = new OaRegAttachmentSyncDO();
        record.setFormMainId(row.formMainId());
        record.setOwnerName(row.ownerName());
        record.setIpdpName(row.ipdpName());
        record.setItemName(row.itemName());
        record.setItemRowId(row.subRowId());
        record.setSharePath(sharePath);
        record.setRetryCount(0);
        return record;
    }

    private OaRegAttachmentSyncDO enrichRecord(OaRegAttachmentSyncDO record, ShareDriveFile file) {
        record.setFileName(file.fileName());
        record.setFileVersion(file.fileVersion());
        record.setFileSize(file.fileSize());
        record.setFileChecksum(file.checksum());
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
