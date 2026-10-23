package com.cacch.integration.manager.oa.api.impl;

import com.cacch.integration.common.config.oa.OaContractAttachmentSyncProperties;
import com.cacch.integration.common.constant.oa.OaContractAttachmentConstants;
import com.cacch.integration.common.dto.oa.OaContractAttachmentSyncResult;
import com.cacch.integration.entity.oa.OaContractAttachmentSyncDO;
import com.cacch.integration.integration.oa.client.OaContractDbClient;
import com.cacch.integration.integration.oa.client.dto.OaContractRow;
import com.cacch.integration.manager.oa.api.IOaContractAttachmentSyncManager;
import com.cacch.integration.service.oa.api.IOaContractAttachmentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OA 货物购销合同附件回传 CRM 编排实现
 *
 * <p>执行锁（Redis 分布式互斥）随处理阶段一并接入；当前阶段由 ESB 侧
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

        // 处理链路（乐观锁抢占 → OA 附件 → CRM 回传 → 状态回写/告警）；
        // 当前阶段不动状态，避免占用记录导致后续轮次无法捞取
        pendingList.forEach(record -> log.info("【{}】待处理记录待后续阶段处理, id={}, oaFormMainId={}, ncOrderNo={}, retryCount={}",
                BIZ, record.getId(), record.getOaFormMainId(), record.getNcOrderNo(), record.getRetryCount()));

        log.info("【{}】本轮执行完成, scanned={}, picked={}, costMs={}",
                BIZ, result.getScanned(), result.getPicked(), System.currentTimeMillis() - startMs);
        return result;
    }
}
