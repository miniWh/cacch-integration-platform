package com.cacch.integration.manager.oa.api.impl;

import com.cacch.integration.common.config.oa.OaContractAttachmentSyncProperties;
import com.cacch.integration.common.constant.oa.OaContractAttachmentConstants;
import com.cacch.integration.common.dto.oa.OaContractAttachmentSyncResult;
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
 * <p>当前实现覆盖 F2：中间表 NOT IN 增量扫描入库。CRM 回传处理阶段（F4-F9：
 * 待处理捞取、乐观锁抢占、线程池并发、状态回写、告警）由后续功能点接入，
 * 本轮 {@code picked} 恒为 0。</p>
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

        log.info("【{}】本轮扫描阶段完成, scanned={}, costMs={}",
                BIZ, result.getScanned(), System.currentTimeMillis() - startMs);
        return result;
    }
}
