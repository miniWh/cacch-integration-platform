package com.cacch.integration.manager.oa.api.impl;

import com.cacch.integration.common.config.oa.OaContractAttachmentSyncProperties;
import com.cacch.integration.common.config.oa.OaProperties;
import com.cacch.integration.common.constant.oa.OaContractAttachmentConstants;
import com.cacch.integration.common.dto.oa.OaContractAttachmentSyncItem;
import com.cacch.integration.common.dto.oa.OaContractAttachmentSyncResult;
import com.cacch.integration.entity.oa.OaContractAttachmentSyncDO;
import com.cacch.integration.integration.oa.client.OaAttachmentDbClient;
import com.cacch.integration.integration.oa.client.OaContractDbClient;
import com.cacch.integration.integration.oa.client.dto.OaAttachmentRow;
import com.cacch.integration.integration.oa.client.dto.OaContractRow;
import com.cacch.integration.manager.oa.api.IOaContractAttachmentSyncManager;
import com.cacch.integration.service.oa.api.IOaContractAttachmentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * OA 货物购销合同附件回传 CRM 编排实现
 *
 * <p>执行链路（REQ-OA-003）：NOT IN 增量扫描入库 → 回收超时 PROCESSING → 捞取
 * PENDING / RETRY → 逐条处理：乐观锁 CAS 抢占 → 按附件引用查 OA 附件 →
 * 拼装下载地址回填中间表（状态 PENDING_CRM）。CRM 合同修改调用（F7）待接入，
 * PENDING_CRM 记录由 CRM 回传阶段捞取消费。</p>
 *
 * <p>当前逐条串行处理（单条 = 1 次 OA 查询 + 1 次状态回写，200 条/10 分钟余量充足）；
 * 线程池并发随 CRM 回传阶段一并接入。</p>
 *
 * <p>执行锁（Redis 分布式互斥）随 CRM 回传阶段一并接入；当前阶段由 ESB 侧
 * 保证调用串行。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaContractAttachmentSyncManagerImpl implements IOaContractAttachmentSyncManager {

    private static final String BIZ = OaContractAttachmentConstants.LOG_BIZ;

    private final OaContractAttachmentSyncProperties properties;
    private final OaContractDbClient oaContractDbClient;
    private final OaAttachmentDbClient oaAttachmentDbClient;
    private final OaProperties oaProperties;
    private final IOaContractAttachmentSyncService syncService;

    @Override
    public OaContractAttachmentSyncResult execute(boolean fullScan, String ncOrderNo) {
        OaContractAttachmentSyncResult result = new OaContractAttachmentSyncResult();
        if (!properties.isEnabled()) {
            log.info("【{}】执行终止, reason=功能已停用", BIZ);
            result.setExecuted(false);
            result.setMessage("功能已停用");
            return result;
        }

        long startMs = System.currentTimeMillis();
        List<Long> excludedIds = fullScan ? List.of() : syncService.listExistingFormMainIds();
        log.info("【{}】开始扫描, fullScan={}, 已入库排除={}, chunkSize={}, ncOrderNo={}",
                BIZ, fullScan, excludedIds.size(), properties.getScanChunkSize(), ncOrderNo);

        List<OaContractRow> rows = oaContractDbClient.scanContractsNotIn(excludedIds, properties.getScanChunkSize());
        result.setScanned(syncService.upsertFromScan(rows));

        // 先回收超时 PROCESSING 僵尸记录，再捞取待处理数据（PENDING / RETRY，不设失败上限）
        int reclaimed = syncService.reclaimTimeoutProcessing(properties.getProcessingTimeoutMinutes());
        List<OaContractAttachmentSyncDO> pendingList = syncService.listPendingForProcess(properties.getBatchSize());
        result.setPicked(pendingList.size());
        log.info("【{}】待处理捞取完成, reclaimed={}, picked={}, limit={}",
                BIZ, reclaimed, pendingList.size(), properties.getBatchSize());

        // 逐条处理：抢占 → 查附件 → 拼地址回填；单条失败不阻断其余条目
        String oaBaseUrl = oaProperties.resolvedBaseUrl();
        for (OaContractAttachmentSyncDO record : pendingList) {
            processOne(record, oaBaseUrl, result);
        }

        log.info("【{}】本轮执行完成, scanned={}, picked={}, readyForCrm={}, skipped={}, failed={}, costMs={}",
                BIZ, result.getScanned(), result.getPicked(),
                result.getSuccessCount(), result.getSkipCount(), result.getFailureCount(),
                System.currentTimeMillis() - startMs);
        return result;
    }

    /**
     * 处理单条待处理记录：CAS 抢占 → 查 OA 附件 → 拼装下载地址回填（→ PENDING_CRM）
     *
     * <p>CRM 合同修改调用为后续阶段；本方法任何分支失败均不抛出，
     * 保证批次内单条隔离。</p>
     *
     * @param record    捞取到的待处理记录
     * @param oaBaseUrl OA 基础地址（已规范化，无末尾斜杠）
     * @param result    本轮执行结果载体，计数与明细写入其中
     */
    private void processOne(OaContractAttachmentSyncDO record, String oaBaseUrl,
                            OaContractAttachmentSyncResult result) {
        Long id = record.getId();
        String ncOrderNo = record.getNcOrderNo();
        try {
            // 1. 乐观锁 CAS 抢占（PENDING/RETRY → PROCESSING），失败说明被其他轮次抢走
            if (!syncService.claimForProcess(id, record.getVersion())) {
                return;
            }
            int claimedVersion = record.getVersion() + 1;
            OaContractAttachmentSyncItem item =
                    new OaContractAttachmentSyncItem(record.getOaFormMainId(), ncOrderNo, null);

            // 2. 按附件引用查 OA 附件（field0360 → ctp_attachment.sub_reference）
            Optional<OaAttachmentRow> attachmentOpt =
                    oaAttachmentDbClient.findFirstAttachmentByRef(record.getOaAttachmentRef());
            if (attachmentOpt.isEmpty()) {
                // 业务性终止：附件缺失 → SKIPPED 终态，不参与重试
                log.info("【{}】记录终止为跳过, id={}, oaFormMainId={}, ncOrderNo={}, reason=OA附件缺失",
                        BIZ, id, record.getOaFormMainId(), ncOrderNo);
                syncService.markSkipped(id, claimedVersion,
                        OaContractAttachmentConstants.SKIP_REASON_ATTACHMENT_MISSING);
                result.recordSkip(item, OaContractAttachmentConstants.SKIP_REASON_ATTACHMENT_MISSING);
                return;
            }

            // 3. 拼装附件下载地址并回填（Q2：不转存，直接回传 OA REST 地址）
            OaAttachmentRow attachment = attachmentOpt.get();
            String attachmentUrl = OaContractAttachmentConstants.buildAttachmentDownloadUrl(
                    oaBaseUrl, attachment.fileUrl());
            boolean ready = syncService.markAttachmentReady(id, claimedVersion,
                    attachment.id(), attachment.fileUrl(), attachment.fileName(), attachmentUrl);
            if (ready) {
                log.info("【{}】附件获取并回填成功, id={}, oaFormMainId={}, ncOrderNo={}, " +
                                "attachmentId={}, fileName={}, url={}",
                        BIZ, id, record.getOaFormMainId(), ncOrderNo,
                        attachment.id(), attachment.fileName(), attachmentUrl);
                result.recordSuccess(item);
            } else {
                // 回填未生效（状态被回收/并发变化）不计入成功，也不置 RETRY
                result.recordSkip(item, "附件就绪回填未生效，状态或版本已变化");
            }
        } catch (Exception e) {
            // 单条失败：尝试回写 RETRY（附带抢占后版本）；回写自身失败仅告警日志，不阻断
            log.warn("【{}】单条处理失败, id={}, oaFormMainId={}, ncOrderNo={}",
                    BIZ, id, record.getOaFormMainId(), ncOrderNo, e);
            try {
                syncService.markRetry(id, record.getVersion() + 1, e.getMessage());
            } catch (Exception markEx) {
                log.error("【{}】失败状态回写异常, id={}, ncOrderNo={}", BIZ, id, ncOrderNo, markEx);
            }
            result.recordFailure(
                    new OaContractAttachmentSyncItem(record.getOaFormMainId(), ncOrderNo, null),
                    e.getMessage());
        }
    }
}
