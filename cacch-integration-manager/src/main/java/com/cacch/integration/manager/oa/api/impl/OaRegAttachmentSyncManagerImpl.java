package com.cacch.integration.manager.oa.api.impl;

import com.cacch.integration.common.config.oa.OaRegAttachmentSyncProperties;
import com.cacch.integration.common.config.sharedrive.ShareDriveProperties;
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
import com.cacch.integration.integration.oa.support.OaRegReportPathSupport;
import com.cacch.integration.integration.sharedrive.client.IShareDriveClient;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
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
import java.util.Optional;
import java.util.Set;

/**
 * 国内登记报告附件同步编排实现
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
    private final ShareDriveProperties shareDriveProperties;
    private final IWeComWebhookManager weComWebhookManager;

    @Override
    public OaRegAttachmentSyncResult syncAttachments(Long formMainId) {
        int batchSize = Math.max(1, syncProperties.getBatchSize());
        int formBatchSize = Math.max(1, syncProperties.getFormBatchSize());
        int maxRetry = syncProperties.getMaxRetry() > 0
                ? syncProperties.getMaxRetry()
                : OaRegReportConstants.DEFAULT_MAX_RETRY;

        List<OaRegReportItemRow> rows = oaRegReportDbClient.listRegReportItems(
                formMainId,
                formBatchSize,
                batchSize,
                syncProperties.hasOwnerNameFilter() ? syncProperties.getOwnerNameFilter() : null);
        if (rows.isEmpty()) {
            log.info("【{}】本轮无资料行可处理, formMainId={}, ownerFilter={}",
                    BIZ, formMainId, syncProperties.getOwnerNameFilter());
            return OaRegAttachmentSyncResult.builder()
                    .scanned(0).success(0).retry(0).failed(0).skipped(0).build();
        }

        if (!shareDriveClient.isAvailable()) {
            log.info("【{}】共享盘不可用，资料行将全部记为跳过", BIZ);
        }

        Set<String> ipdpPathCollisionKeys = detectIpdpPathCollisions(rows);

        log.info("【{}】开始附件同步, formMainId={}, scanned={}, formBatchSize={}, subRowBatchSize={}, maxRetry={}, ipdpCollisions={}",
                BIZ, formMainId, rows.size(), formBatchSize, batchSize, maxRetry, ipdpPathCollisionKeys.size());

        int success = 0;
        int retry = 0;
        int failed = 0;
        int skipped = 0;

        String rootPath = OaRegReportPathSupport.resolveRootPath(shareDriveProperties);

        for (OaRegReportItemRow row : rows) {
            try {
                String outcome = syncOneRow(row, rootPath, maxRetry, ipdpPathCollisionKeys);
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
                log.info("【{}】单条同步异常终止, formMainId={}, subRowId={}, reason={}",
                        BIZ, row.formMainId(), row.subRowId(), e.getMessage());
                log.error("【{}】单条同步失败, formMainId={}, subRowId={}",
                        BIZ, row.formMainId(), row.subRowId(), e);
                OaRegAttachmentSyncDO record = baseRecord(row, null);
                String status = syncService.markFailure(record, "同步异常: " + e.getMessage(), maxRetry);
                if (OaRegAttachmentSyncStatusEnum.FAILED.getCode().equals(status)) {
                    failed++;
                    alertFailed(row, e.getMessage());
                } else {
                    retry++;
                }
            }
        }

        OaRegAttachmentSyncResult result = OaRegAttachmentSyncResult.builder()
                .scanned(rows.size())
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

    private String syncOneRow(OaRegReportItemRow row,
                              String rootPath,
                              int maxRetry,
                              Set<String> ipdpPathCollisionKeys) {
        if (row == null || row.subRowId() == null || row.formMainId() == null) {
            log.info("【{}】跳过同步, reason=行数据不完整", BIZ);
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }
        if (!StringUtils.hasText(row.ownerName()) || !StringUtils.hasText(row.ipdpName())
                || !StringUtils.hasText(row.itemName())) {
            log.info("【{}】跳过同步, reason=名称字段为空, subRowId={}", BIZ, row.subRowId());
            OaRegAttachmentSyncDO record = baseRecord(row, null);
            syncService.markSkipped(record, "名称字段为空");
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }
        if (looksLikeMemberId(row.ownerName())) {
            log.info("【{}】跳过同步, reason=登记负责人未解析为姓名, subRowId={}, rawOwner={}",
                    BIZ, row.subRowId(), row.ownerName());
            OaRegAttachmentSyncDO record = baseRecord(row, null);
            syncService.markSkipped(record, OaRegReportConstants.SKIP_OWNER_UNRESOLVED);
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        String ipdpKey = OaRegReportPathSupport.buildNormalizedIpdpKey(row.ownerName(), row.ipdpName());
        if (ipdpPathCollisionKeys.contains(ipdpKey)) {
            log.info("【{}】跳过同步, reason=IPDP归一化路径冲突, subRowId={}, oaIpdp={}",
                    BIZ, row.subRowId(), row.ipdpName());
            OaRegAttachmentSyncDO record = baseRecord(row, null);
            syncService.markSkipped(record, ShareDriveConstants.SKIP_PATH_COLLISION + ":" + row.ipdpName());
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        String sharePath = OaRegReportPathSupport.buildItemDirectory(
                rootPath, row.ownerName(), row.ipdpName(), row.itemName());
        log.info("【{}】拼共享盘路径, subRowId={}, oaIpdp={}, sharePath={}",
                BIZ, row.subRowId(), row.ipdpName(), sharePath);

        if (!shareDriveClient.isAvailable()) {
            log.info("【{}】跳过同步, reason=共享盘未就绪, subRowId={}, path={}",
                    BIZ, row.subRowId(), sharePath);
            OaRegAttachmentSyncDO record = baseRecord(row, sharePath);
            syncService.markSkipped(record, OaRegReportConstants.SKIP_MISSING_DIR + ":共享盘未就绪");
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        Optional<ShareDriveFile> fileOpt = shareDriveClient.pickLatestVersion(sharePath);
        if (fileOpt.isEmpty()) {
            log.info("【{}】跳过同步, reason=目录无文件, subRowId={}, path={}",
                    BIZ, row.subRowId(), sharePath);
            OaRegAttachmentSyncDO record = baseRecord(row, sharePath);
            syncService.markSkipped(record, OaRegReportConstants.SKIP_NO_FILE);
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        ShareDriveFile file = fileOpt.get();
        OaRegAttachmentSyncDO existing = syncService.findByBizKey(
                row.ownerName(), row.ipdpName(), row.itemName(), file.fileVersion());
        if (syncService.shouldSkipSuccess(existing, file.checksum())) {
            log.info("【{}】幂等跳过, subRowId={}, item={}, version={}",
                    BIZ, row.subRowId(), row.itemName(), file.fileVersion());
            return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
        }

        if (file.content() == null || file.content().length == 0) {
            log.info("【{}】跳过同步, reason=文件内容为空, subRowId={}, fileName={}",
                    BIZ, row.subRowId(), file.fileName());
            OaRegAttachmentSyncDO record = enrichRecord(baseRecord(row, sharePath), file);
            syncService.markSkipped(record, OaRegReportConstants.SKIP_NO_FILE + ":文件内容为空");
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

            OaRegAttachmentSyncDO record = enrichRecord(baseRecord(row, sharePath), file);
            record.setOaFileId(bindResult.fileUrl());
            record.setOaSubReference(bindResult.subReference());
            syncService.markSuccess(record);
            log.info("【{}】同步成功, formMainId={}, subRowId={}, item={}, fileUrl={}",
                    BIZ, row.formMainId(), row.subRowId(), row.itemName(), bindResult.fileUrl());
            return OaRegAttachmentSyncStatusEnum.SUCCESS.getCode();
        } catch (BizException e) {
            log.info("【{}】同步业务失败, subRowId={}, item={}, reason={}",
                    BIZ, row.subRowId(), row.itemName(), e.getMessage());
            OaRegAttachmentSyncDO record = enrichRecord(baseRecord(row, sharePath), file);
            String status = syncService.markFailure(record, e.getMessage(), maxRetry);
            if (OaRegAttachmentSyncStatusEnum.FAILED.getCode().equals(status)) {
                alertFailed(row, e.getMessage());
            }
            return status;
        }*/
        return OaRegAttachmentSyncStatusEnum.SKIPPED.getCode();
    }

    /**
     * 检测同一负责人下不同 OA IPDP 名称归一化后指向同一路径的冲突
     *
     * @param rows 资料行
     * @return 发生冲突的归一化 IPDP 键集合
     */
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
                log.info("【{}】检测到 IPDP 路径冲突, key={}, oaIpdp1={}, oaIpdp2={}",
                        BIZ, key, previous, row.ipdpName());
            }
        }
        return collisions;
    }

    /**
     * 判断登记负责人是否仍为 OA 人员 ID（org_member JOIN 未解析出姓名）
     *
     * @param ownerName 登记负责人
     * @return true 表示形如纯数字 ID
     */
    private static boolean looksLikeMemberId(String ownerName) {
        return StringUtils.hasText(ownerName) && ownerName.trim().matches("-?\\d+");
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
