package com.cacch.integration.manager.oa.api.impl;

import com.cacch.integration.common.config.oa.ShareDirProvisionProperties;
import com.cacch.integration.common.config.sharedrive.ShareDriveProperties;
import com.cacch.integration.common.constant.oa.ShareDirProvisionConstants;
import com.cacch.integration.common.dto.oa.OaRegShareDirProvisionResult;
import com.cacch.integration.common.dto.wecom.WeComAlertCommand;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.oa.OaRegShareDirProvisionDO;
import com.cacch.integration.integration.oa.client.OaRegReportDbClient;
import com.cacch.integration.integration.oa.client.dto.OaRegReportItemRow;
import com.cacch.integration.integration.oa.support.OaIdSupport;
import com.cacch.integration.integration.oa.support.OaRegItemRequiredSupport;
import com.cacch.integration.integration.oa.support.OaRegReportPathSupport;
import com.cacch.integration.integration.sharedrive.client.IShareDriveClient;
import com.cacch.integration.integration.sharedrive.support.ShareDriveIpdpDirectorySupport;
import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import com.cacch.integration.manager.oa.api.IOaRegShareDirectoryProvisionManager;
import com.cacch.integration.manager.wecom.api.IWeComWebhookManager;
import com.cacch.integration.service.oa.api.IOaRegShareDirProvisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 共享盘目录治理编排实现（REQ-OA-002）
 *
 * <p>核心流程：Redis 游标分批拉取 OA 资料行 → 归一化路径段 + itemRequired →
 * 按 L3 sharePath 分组聚合 groupRetain → SMB 探测 → mkdir / 删空 L3 →
 * 写治理记录 → 统计与告警。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OaRegShareDirectoryProvisionManagerImpl implements IOaRegShareDirectoryProvisionManager {

    private static final String BIZ = ShareDirProvisionConstants.LOG_BIZ;

    private final OaRegReportDbClient oaRegReportDbClient;
    private final IShareDriveClient shareDriveClient;
    private final IOaRegShareDirProvisionService provisionService;
    private final ShareDirProvisionProperties provisionProperties;
    private final ShareDriveProperties shareDriveProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final IWeComWebhookManager weComWebhookManager;

    /**
     * 归一化后的资料行（含 itemRequired 判定与 sharePath）
     */
    private record NormalizedRow(
            OaRegReportItemRow original,
            boolean normalizedRequired,
            String sharePath
    ) {
    }

    /**
     * 路径组决策结果
     */
    private record GroupDecision(
            String sharePath,
            boolean groupRetain,
            String action,
            String actionMessage
    ) {
    }

    @Override
    public OaRegShareDirProvisionResult provisionDirectories(Long formMainId) {
        String runId = generateRunId();
        log.info("【{}】目录治理开始, runId={}, formMainId={}", BIZ, runId, formMainId);

        if (!shareDriveClient.isAvailable()) {
            log.info("【{}】共享盘不可用, 本轮治理终止, runId={}", BIZ, runId);
            alertSmbUnavailable(runId);
            return emptyResult();
        }

        String rootPath = shareDriveProperties.getRootPath().trim();
        List<String> formMainIds = resolveCursorBatchFormMainIds(formMainId);
        if (formMainIds.isEmpty()) {
            log.info("【{}】主表批次为空, 本轮无处理, runId={}", BIZ, runId);
            return emptyResult();
        }

        List<OaRegReportItemRow> itemRows = oaRegReportDbClient.listItemRowsByFormMainIds(formMainIds);
        log.info("【{}】OA资料行已加载, runId={}, formCount={}, rowCount={}", BIZ, runId, formMainIds.size(), itemRows.size());

        if (itemRows.isEmpty()) {
            log.info("【{}】OA资料行为空, 本轮无处理, runId={}", BIZ, runId);
            return emptyResult();
        }

        Set<String> ownerAllowlist = provisionProperties.getOwnerAllowlist();
        Set<String> ignoreSystemFiles = provisionProperties.getIgnoreSystemFiles();

        List<OaRegShareDirProvisionDO> failedRecords = new ArrayList<>();
        Map<String, List<NormalizedRow>> pathGroups = new LinkedHashMap<>();

        normalizeAndGroup(itemRows, rootPath, ownerAllowlist, runId, failedRecords, pathGroups);

        List<OaRegShareDirProvisionDO> groupRecords = new ArrayList<>();
        int created = 0, deleted = 0, skippedExists = 0, skippedNotEmpty = 0,
                skippedNotRequired = 0, skippedGroupRetained = 0, failed = failedRecords.size();

        for (Map.Entry<String, List<NormalizedRow>> entry : pathGroups.entrySet()) {
            String sharePath = entry.getKey();
            List<NormalizedRow> groupRows = entry.getValue();
            boolean groupRetain = groupRows.stream().anyMatch(NormalizedRow::normalizedRequired);

            GroupDecision decision = probeAndAct(sharePath, groupRetain, ignoreSystemFiles, runId);

            switch (decision.action()) {
                case ShareDirProvisionConstants.ACTION_CREATED -> created++;
                case ShareDirProvisionConstants.ACTION_DELETED -> deleted++;
                case ShareDirProvisionConstants.ACTION_SKIPPED_EXISTS -> skippedExists++;
                case ShareDirProvisionConstants.ACTION_SKIPPED_NOT_EMPTY -> skippedNotEmpty++;
                case ShareDirProvisionConstants.ACTION_SKIPPED_NOT_REQUIRED -> skippedNotRequired++;
                case ShareDirProvisionConstants.ACTION_FAILED -> failed++;
            }

            for (NormalizedRow nr : groupRows) {
                OaRegShareDirProvisionDO record = buildRecord(nr, groupRetain, decision, runId);
                groupRecords.add(record);

                if (ShareDirProvisionConstants.ACTION_SKIPPED_GROUP_RETAINED.equals(record.getAction())) {
                    skippedGroupRetained++;
                }
            }
        }

        List<OaRegShareDirProvisionDO> allRecords = new ArrayList<>(failedRecords);
        allRecords.addAll(groupRecords);

        if (!allRecords.isEmpty()) {
            provisionService.batchInsert(allRecords);
        }

        OaRegShareDirProvisionResult result = OaRegShareDirProvisionResult.builder()
                .totalRows(itemRows.size())
                .totalGroups(pathGroups.size())
                .created(created)
                .deleted(deleted)
                .skippedExists(skippedExists)
                .skippedNotEmpty(skippedNotEmpty)
                .skippedNotRequired(skippedNotRequired)
                .skippedGroupRetained(skippedGroupRetained)
                .failed(failed)
                .build();

        log.info("【{}】目录治理完成, runId={}, totalRows={}, totalGroups={}, created={}, deleted={}, "
                        + "skippedExists={}, skippedNotEmpty={}, skippedNotRequired={}, skippedGroupRetained={}, failed={}",
                BIZ, runId, result.getTotalRows(), result.getTotalGroups(), result.getCreated(),
                result.getDeleted(), result.getSkippedExists(), result.getSkippedNotEmpty(),
                result.getSkippedNotRequired(), result.getSkippedGroupRetained(), result.getFailed());

        if (skippedNotEmpty > 0 && provisionProperties.isAlertNotEmptySkipped()) {
            alertNotEmptySkipped(runId, skippedNotEmpty);
        }
        if (failed > 0) {
            alertFailed(runId, failed);
        }

        return result;
    }

    // ── 归一化与分组 ──

    /**
     * 遍历 OA 资料行，归一化路径段与 itemRequired，按 sharePath 分组；
     * 归一化失败的行直接生成 FAILED 记录
     */
    private void normalizeAndGroup(List<OaRegReportItemRow> itemRows,
                                   String rootPath,
                                   Set<String> ownerAllowlist,
                                   String runId,
                                   List<OaRegShareDirProvisionDO> failedRecords,
                                   Map<String, List<NormalizedRow>> pathGroups) {
        LocalDateTime now = LocalDateTime.now();
        for (OaRegReportItemRow row : itemRows) {
            if (!StringUtils.hasText(row.ownerName()) || looksLikeMemberId(row.ownerName())) {
                log.info("【{}】跳过行, reason=登记负责人未解析为姓名, owner={}", BIZ, row.ownerName());
                failedRecords.add(buildFailedRecord(row, "登记负责人未解析为姓名", runId, now));
                continue;
            }
            if (!ownerAllowlist.isEmpty() && !ownerAllowlist.contains(row.ownerName().trim())) {
                log.info("【{}】跳过行, reason=不在白名单, owner={}", BIZ, row.ownerName());
                continue;
            }

            String normalizedOwner = ShareDrivePathNormalizer.normalize(row.ownerName());
            String normalizedIpdpName = ShareDrivePathNormalizer.normalize(row.ipdpName());
            String normalizedProjectNo = ShareDriveIpdpDirectorySupport.normalizeProjectNo(row.ipdpProjectNo());
            String normalizedItem = ShareDrivePathNormalizer.normalize(row.itemName());

            if (!StringUtils.hasText(normalizedOwner) || !StringUtils.hasText(normalizedIpdpName)
                    || !StringUtils.hasText(normalizedProjectNo) || !StringUtils.hasText(normalizedItem)) {
                log.info("【{}】归一化失败, reason=路径段为空, owner={}, ipdp={}, projectNo={}, item={}",
                        BIZ, row.ownerName(), row.ipdpName(), row.ipdpProjectNo(), row.itemName());
                failedRecords.add(buildFailedRecord(row, "路径段归一化后为空", runId, now));
                continue;
            }

            String l2DirName = ShareDriveIpdpDirectorySupport.formatDirectoryName(
                    normalizedIpdpName, normalizedProjectNo);
            String sharePath = OaRegReportPathSupport.buildItemDirectory(
                    rootPath, normalizedOwner, l2DirName, normalizedItem);

            boolean normalizedRequired = OaRegItemRequiredSupport.isRequired(row.itemRequired());
            pathGroups.computeIfAbsent(sharePath, k -> new ArrayList<>())
                    .add(new NormalizedRow(row, normalizedRequired, sharePath));
        }
    }

    // ── SMB 探测与执行 ──

    /**
     * 按路径组探测共享盘并执行创建/删除/跳过
     */
    private GroupDecision probeAndAct(String sharePath,
                                      boolean groupRetain,
                                      Set<String> ignoreSystemFiles,
                                      String runId) {
        try {
            boolean exists = shareDriveClient.existsDirectory(sharePath);

            if (groupRetain) {
                if (!exists) {
                    shareDriveClient.mkdirs(sharePath);
                    log.info("【{}】目录创建成功, runId={}, sharePath={}", BIZ, runId, sharePath);
                    return new GroupDecision(sharePath, true,
                            ShareDirProvisionConstants.ACTION_CREATED, "L1/L2/L3 逐级创建");
                }
                log.info("【{}】目录已存在, 跳过创建, runId={}, sharePath={}", BIZ, runId, sharePath);
                return new GroupDecision(sharePath, true,
                        ShareDirProvisionConstants.ACTION_SKIPPED_EXISTS, "L3 已存在");
            }

            if (!exists) {
                log.info("【{}】不需要且目录不存在, 跳过, runId={}, sharePath={}", BIZ, runId, sharePath);
                return new GroupDecision(sharePath, false,
                        ShareDirProvisionConstants.ACTION_SKIPPED_NOT_REQUIRED, "组内全部不需要, L3 不存在");
            }

            if (!shareDriveClient.isEmptyDirectory(sharePath, ignoreSystemFiles)) {
                log.info("【{}】不需要但目录非空, 跳过删除, runId={}, sharePath={}", BIZ, runId, sharePath);
                return new GroupDecision(sharePath, false,
                        ShareDirProvisionConstants.ACTION_SKIPPED_NOT_EMPTY, "L3 非空, 需人工清理");
            }

            shareDriveClient.deleteEmptyDirectory(sharePath);
            log.info("【{}】空目录删除成功, runId={}, sharePath={}", BIZ, runId, sharePath);
            return new GroupDecision(sharePath, false,
                    ShareDirProvisionConstants.ACTION_DELETED, "L3 空目录已删除");

        } catch (BizException e) {
            log.error("【{}】SMB 操作失败, runId={}, sharePath={}, reason={}", BIZ, runId, sharePath, e.getMessage(), e);
            return new GroupDecision(sharePath, groupRetain,
                    ShareDirProvisionConstants.ACTION_FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("【{}】SMB 操作异常, runId={}, sharePath={}", BIZ, runId, sharePath, e);
            return new GroupDecision(sharePath, groupRetain,
                    ShareDirProvisionConstants.ACTION_FAILED, e.getMessage());
        }
    }

    // ── 记录构建 ──

    /**
     * 根据路径组决策为每行构建治理记录；
     * 「不需要」且 groupRetain=true 的行 → SKIPPED_GROUP_RETAINED
     */
    private OaRegShareDirProvisionDO buildRecord(NormalizedRow nr,
                                                 boolean groupRetain,
                                                 GroupDecision decision,
                                                 String runId) {
        OaRegReportItemRow row = nr.original();
        OaRegShareDirProvisionDO record = new OaRegShareDirProvisionDO();
        record.setRunId(runId);
        record.setFormMainId(OaIdSupport.toStorageLong(row.formMainId()));
        record.setOwnerName(row.ownerName());
        record.setIpdpName(row.ipdpName());
        record.setIpdpProjectNo(row.ipdpProjectNo());
        record.setItemName(row.itemName());
        record.setItemRowId(row.subRowId());
        record.setItemRequired(row.itemRequired());
        record.setSharePath(nr.sharePath());
        record.setGroupRetain(groupRetain);
        record.setProvisionedAt(LocalDateTime.now());

        if (!nr.normalizedRequired() && groupRetain) {
            record.setAction(ShareDirProvisionConstants.ACTION_SKIPPED_GROUP_RETAINED);
            record.setActionMessage("由同路径组其他「需要」行触发保留");
        } else {
            record.setAction(decision.action());
            record.setActionMessage(decision.actionMessage());
        }
        return record;
    }

    private OaRegShareDirProvisionDO buildFailedRecord(OaRegReportItemRow row, String reason,
                                                       String runId, LocalDateTime now) {
        OaRegShareDirProvisionDO record = new OaRegShareDirProvisionDO();
        record.setRunId(runId);
        record.setFormMainId(OaIdSupport.toStorageLong(row.formMainId()));
        record.setOwnerName(row.ownerName());
        record.setIpdpName(row.ipdpName());
        record.setIpdpProjectNo(row.ipdpProjectNo());
        record.setItemName(row.itemName());
        record.setItemRowId(row.subRowId());
        record.setItemRequired(row.itemRequired());
        record.setSharePath(null);
        record.setGroupRetain(null);
        record.setAction(ShareDirProvisionConstants.ACTION_FAILED);
        record.setActionMessage(reason);
        record.setProvisionedAt(now);
        return record;
    }

    // ── 游标管理 ──

    private List<String> resolveCursorBatchFormMainIds(Long formMainId) {
        if (formMainId != null && formMainId > 0) {
            return List.of(String.valueOf(formMainId));
        }
        String cursor = getLastProvisionCursor();
        List<String> batch = oaRegReportDbClient.listFormMainIdsAfterCursor(
                cursor, provisionProperties.getFormBatchSize(), null);
        if (batch.isEmpty() && StringUtils.hasText(cursor) && !"0".equals(cursor)) {
            log.info("【{}】游标已至末尾, 从头开始新一轮, previousCursor={}", BIZ, cursor);
            resetProvisionCursor();
            batch = oaRegReportDbClient.listFormMainIdsAfterCursor(
                    "0", provisionProperties.getFormBatchSize(), null);
        }
        if (!batch.isEmpty()) {
            String newCursor = batch.get(batch.size() - 1);
            saveProvisionCursor(newCursor);
            log.info("【{}】游标批次推进, previousCursor={}, newCursor={}, formCount={}",
                    BIZ, cursor, newCursor, batch.size());
        }
        return batch;
    }

    private String getLastProvisionCursor() {
        String raw = stringRedisTemplate.opsForValue().get(ShareDirProvisionConstants.PROVISION_CURSOR_REDIS_KEY);
        return StringUtils.hasText(raw) ? raw.trim() : "0";
    }

    private void saveProvisionCursor(String cursor) {
        if (StringUtils.hasText(cursor)) {
            stringRedisTemplate.opsForValue().set(
                    ShareDirProvisionConstants.PROVISION_CURSOR_REDIS_KEY, cursor.trim());
        }
    }

    private void resetProvisionCursor() {
        stringRedisTemplate.delete(ShareDirProvisionConstants.PROVISION_CURSOR_REDIS_KEY);
    }

    // ── 告警 ──

    private void alertSmbUnavailable(String runId) {
        weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                .biz("oa")
                .title("共享盘目录治理：SMB 不可用")
                .subject("目录治理任务终止")
                .context("runId=" + runId)
                .errorMessage("共享盘不可用, 无法执行目录治理")
                .dedupType("oa-share-dir-provision-smb")
                .dedupId("smb-unavailable")
                .build());
    }

    private void alertNotEmptySkipped(String runId, int count) {
        weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                .biz("oa")
                .title("共享盘目录治理：非空目录跳过删除")
                .subject(count + " 个「不需要」L3 目录非空, 已跳过删除")
                .context("runId=" + runId + ", 请负责人手动清理后下轮自动删除")
                .dedupType("oa-share-dir-provision-not-empty")
                .dedupId(runId)
                .build());
    }

    private void alertFailed(String runId, int failedCount) {
        weComWebhookManager.sendAlert(WeComAlertCommand.builder()
                .biz("oa")
                .title("共享盘目录治理：存在失败记录")
                .subject(failedCount + " 条记录处理失败")
                .context("runId=" + runId)
                .dedupType("oa-share-dir-provision-failed")
                .dedupId(runId)
                .mention(true)
                .build());
    }

    // ── 工具 ──

    private static boolean looksLikeMemberId(String ownerName) {
        return StringUtils.hasText(ownerName) && ownerName.trim().matches("-?\\d+");
    }

    private static String generateRunId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static OaRegShareDirProvisionResult emptyResult() {
        return OaRegShareDirProvisionResult.builder()
                .totalRows(0).totalGroups(0).created(0).deleted(0)
                .skippedExists(0).skippedNotEmpty(0).skippedNotRequired(0)
                .skippedGroupRetained(0).failed(0)
                .build();
    }
}
