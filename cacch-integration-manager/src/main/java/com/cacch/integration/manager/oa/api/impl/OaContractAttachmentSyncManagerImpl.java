package com.cacch.integration.manager.oa.api.impl;

import com.cacch.integration.common.config.crm.CrmContractProperties;
import com.cacch.integration.common.config.oa.OaContractAttachmentSyncProperties;
import com.cacch.integration.common.constant.crm.CrmContractConstants;
import com.cacch.integration.common.constant.oa.OaContractAttachmentConstants;
import com.cacch.integration.common.dto.oa.OaContractAttachmentSyncItem;
import com.cacch.integration.common.dto.oa.OaContractAttachmentSyncResult;
import com.cacch.integration.entity.oa.OaContractAttachmentSyncDO;
import com.cacch.integration.integration.crm.client.CrmClient;
import com.cacch.integration.integration.crm.client.dto.CrmContractEditRequest;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.fileserver.client.FileServerUploadClient;
import com.cacch.integration.integration.oa.client.OaAttachmentDbClient;
import com.cacch.integration.integration.oa.client.OaContractDbClient;
import com.cacch.integration.integration.oa.client.dto.OaAttachmentRow;
import com.cacch.integration.integration.oa.client.dto.OaContractRow;
import com.cacch.integration.manager.oa.api.IOaContractAttachmentSyncManager;
import com.cacch.integration.service.oa.api.IOaContractAttachmentSyncService;
import com.cacch.integration.service.oa.api.IOaTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OA 货物购销合同附件回传 CRM 编排实现
 *
 * <p>执行链路（REQ-OA-003）：NOT IN 增量扫描入库 → 回收超时 PROCESSING →
 * <strong>附件阶段</strong>捞取 PENDING / RETRY：乐观锁 CAS 抢占 → CRM 合同 ID 空校验 →
 * 按附件引用查 OA 附件 → 上传公司内部文件服务器（NFS 挂载：OA 统一鉴权下载附件流 →
 * 落盘挂载目录 → 前缀拼接可访问 URL）→ 回填中间表（状态 PENDING_CRM）→
 * <strong>CRM 回传阶段</strong>捞取 PENDING_CRM：调用 contractEdit 回传附件 URL，
 * 成功置 SUCCESS，失败置 RETRY（无次数上限）。</p>
 *
 * <p>CRM 回传阶段不做二次抢占：contractEdit 幂等（重复写同一附件 URL 无副作用），
 * 直接以捞取时的 version CAS 回写最终状态。PENDING_CRM 回传失败回写 RETRY 后，
 * 下次执行由主捞取入口重新捞取并完整重走附件与回传链路。</p>
 *
 * <p><strong>CRM 合同 ID 存量补偿</strong>：field0495 标量子查询上线前入库的记录
 * {@code crm_contract_id} 为空，且 NOT IN 增量扫描不会重刷已入库记录；两个阶段在
 * CRM 合同 ID 为空时均按 NC 单号反查 OA 回写补偿——附件阶段补偿命中后继续处理、
 * 未命中置 SKIPPED；CRM 回传阶段补偿命中后继续回传、未命中置 RETRY 持续重试。</p>
 *
 * <p>当前逐条串行处理（单条 = 1 次 OA 查询 + 1 次文件服务器上传 + 1 次 CRM 调用，
 * 200 条/10 分钟余量充足）；线程池并发与 Redis 执行锁延后接入，当前由 ESB 侧保证调用串行。</p>
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
    private final FileServerUploadClient fileServerUploadClient;
    private final CrmClient crmClient;
    private final CrmContractProperties crmContractProperties;
    private final IOaTokenService oaTokenService;
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

        // 附件阶段：抢占 → CRM 合同 ID 校验 → 查 OA 附件 → 上传文件服务器 → 回填 PENDING_CRM
        int readyForCrm = 0;
        for (OaContractAttachmentSyncDO record : pendingList) {
            if (processOne(record, result)) {
                readyForCrm++;
            }
        }

        // CRM 回传阶段：捞取 PENDING_CRM → contractEdit 回传 → SUCCESS / RETRY（无二次抢占）
        List<OaContractAttachmentSyncDO> pendingCrmList =
                syncService.listPendingCrmForPush(properties.getBatchSize());
        log.info("【{}】待CRM回传捞取完成, picked={}", BIZ, pendingCrmList.size());
        for (OaContractAttachmentSyncDO record : pendingCrmList) {
            processCrmOne(record, result);
        }

        log.info("【{}】本轮执行完成, scanned={}, picked={}, readyForCrm={}, pushed={}, skipped={}, failed={}, costMs={}",
                BIZ, result.getScanned(), result.getPicked(), readyForCrm,
                result.getSuccessCount(), result.getSkipCount(), result.getFailureCount(),
                System.currentTimeMillis() - startMs);
        return result;
    }

    /**
     * 附件阶段处理单条待处理记录：CAS 抢占 → CRM 合同 ID 校验 → 查 OA 附件 →
     * 上传文件服务器 → 回填 PENDING_CRM
     *
     * <p>本方法任何分支失败均不抛出，保证批次内单条隔离。</p>
     *
     * @param record 捞取到的待处理记录
     * @param result 本轮执行结果载体，计数与明细写入其中
     * @return true 表示附件就绪并回填 PENDING_CRM 成功
     */
    private boolean processOne(OaContractAttachmentSyncDO record, OaContractAttachmentSyncResult result) {
        Long id = record.getId();
        String ncOrderNo = record.getNcOrderNo();
        OaContractAttachmentSyncItem item =
                new OaContractAttachmentSyncItem(record.getOaFormMainId(), ncOrderNo, record.getCrmContractId());
        try {
            // 1. 乐观锁 CAS 抢占（PENDING/RETRY → PROCESSING），失败说明被其他轮次抢走
            if (!syncService.claimForProcess(id, record.getVersion())) {
                return false;
            }
            int claimedVersion = record.getVersion() + 1;

            // 2. CRM 合同数据 ID 校验与存量补偿（crm_contract_id 为空时按 NC 单号反查 OA 回写）
            String crmContractId = record.getCrmContractId();
            if (!StringUtils.hasText(crmContractId)) {
                crmContractId = compensateCrmContractId(record, claimedVersion);
                if (!StringUtils.hasText(crmContractId)) {
                    // 业务性终止：CRM 合同 ID 为空且反查未命中 → SKIPPED 终态，不参与重试
                    log.info("【{}】记录终止为跳过, id={}, oaFormMainId={}, ncOrderNo={}, reason={}",
                            BIZ, id, record.getOaFormMainId(), ncOrderNo,
                            OaContractAttachmentConstants.SKIP_REASON_CRM_CONTRACT_ID_MISSING);
                    syncService.markSkipped(id, claimedVersion,
                            OaContractAttachmentConstants.SKIP_REASON_CRM_CONTRACT_ID_MISSING);
                    result.recordSkip(item, OaContractAttachmentConstants.SKIP_REASON_CRM_CONTRACT_ID_MISSING);
                    return false;
                }
            }

            // 3. 按附件引用查 OA 附件（field0360 → ctp_attachment.sub_reference）
            Optional<OaAttachmentRow> attachmentOpt =
                    oaAttachmentDbClient.findFirstAttachmentByRef(record.getOaAttachmentRef());
            if (attachmentOpt.isEmpty()) {
                // 业务性终止：附件缺失 → SKIPPED 终态，不参与重试
                log.info("【{}】记录终止为跳过, id={}, oaFormMainId={}, ncOrderNo={}, reason={}",
                        BIZ, id, record.getOaFormMainId(), ncOrderNo,
                        OaContractAttachmentConstants.SKIP_REASON_ATTACHMENT_MISSING);
                syncService.markSkipped(id, claimedVersion,
                        OaContractAttachmentConstants.SKIP_REASON_ATTACHMENT_MISSING);
                result.recordSkip(item, OaContractAttachmentConstants.SKIP_REASON_ATTACHMENT_MISSING);
                return false;
            }

            // 4. 上传公司内部文件服务器（NFS 挂载）：OA 统一鉴权下载附件 → 落盘挂载目录 →
            //    前缀拼接可访问 URL；下载/落盘失败抛异常，由外层 catch 统一回写 RETRY
            OaAttachmentRow attachment = attachmentOpt.get();
            String oaToken = oaTokenService.getToken(null);
            String attachmentUrl = fileServerUploadClient.upload(oaToken, attachment.fileName(), attachment.fileUrl());

            // 5. 回填附件信息与 URL（→ PENDING_CRM）
            boolean ready = syncService.markAttachmentReady(id, claimedVersion,
                    attachment.id(), attachment.fileUrl(), attachment.fileName(), attachmentUrl);
            if (ready) {
                log.info("【{}】附件获取并回填成功, id={}, oaFormMainId={}, ncOrderNo={}, " +
                                "attachmentId={}, fileName={}, url={}",
                        BIZ, id, record.getOaFormMainId(), ncOrderNo,
                        attachment.id(), attachment.fileName(), attachmentUrl);
                return true;
            }
            // 回填未生效（状态被回收/并发变化）不计入成功，也不置 RETRY
            result.recordSkip(item, "附件就绪回填未生效，状态或版本已变化");
            return false;
        } catch (Exception e) {
            // 单条失败：尝试回写 RETRY（附带抢占后版本）；回写自身失败仅告警日志，不阻断
            log.warn("【{}】单条处理失败, id={}, oaFormMainId={}, ncOrderNo={}",
                    BIZ, id, record.getOaFormMainId(), ncOrderNo, e);
            try {
                syncService.markRetry(id, record.getVersion() + 1, e.getMessage());
            } catch (Exception markEx) {
                log.error("【{}】失败状态回写异常, id={}, ncOrderNo={}", BIZ, id, ncOrderNo, markEx);
            }
            result.recordFailure(item, e.getMessage());
            return false;
        }
    }

    /**
     * CRM 回传阶段处理单条 PENDING_CRM 记录：组装 contractEdit 请求 → 回传附件 URL →
     * 成功置 SUCCESS / 失败置 RETRY
     *
     * <p>不做二次抢占，直接以捞取时的 version CAS 回写；本方法任何分支失败均不抛出。</p>
     *
     * @param record 捞取到的待 CRM 回传记录
     * @param result 本轮执行结果载体，计数与明细写入其中
     */
    private void processCrmOne(OaContractAttachmentSyncDO record, OaContractAttachmentSyncResult result) {
        Long id = record.getId();
        String ncOrderNo = record.getNcOrderNo();
        OaContractAttachmentSyncItem item =
                new OaContractAttachmentSyncItem(record.getOaFormMainId(), ncOrderNo, record.getCrmContractId());
        try {
            // 1. CRM 合同数据 ID 校验与存量补偿（crm_contract_id 为空时按 NC 单号反查 OA 回写；
            //    存量记录补偿后继续回传，未命中置 RETRY 保持无上限重试）
            String crmContractId = record.getCrmContractId();
            if (!StringUtils.hasText(crmContractId)) {
                crmContractId = compensateCrmContractId(record, record.getVersion());
                if (!StringUtils.hasText(crmContractId)) {
                    String message = "CRM合同ID为空且OA反查未命中";
                    log.info("【{}】CRM回传终止为重试, id={}, oaFormMainId={}, ncOrderNo={}, reason={}",
                            BIZ, id, record.getOaFormMainId(), ncOrderNo, message);
                    syncService.markCrmRetry(id, record.getVersion(), message);
                    result.recordFailure(item, message);
                    return;
                }
                item = new OaContractAttachmentSyncItem(record.getOaFormMainId(), ncOrderNo, crmContractId);
            }

            // 2. 回传前置字段校验（attachmentUrl / fileName 由附件阶段回填，缺失置 RETRY 便于观测）
            if (!StringUtils.hasText(record.getAttachmentUrl())
                    || !StringUtils.hasText(record.getAttachmentFileName())) {
                log.info("【{}】CRM回传终止为重试, id={}, oaFormMainId={}, ncOrderNo={}, " +
                                "crmContractId={}, attachmentUrl={}, fileName={}",
                        BIZ, id, record.getOaFormMainId(), ncOrderNo,
                        crmContractId, record.getAttachmentUrl(), record.getAttachmentFileName());
                syncService.markCrmRetry(id, record.getVersion(), "CRM回传前置字段缺失");
                result.recordFailure(item, "CRM回传前置字段缺失");
                return;
            }

            // 3. 组装 contractEdit 请求（data_id = field0495；附件字段 [{path, name}]，不传 fileSize）
            Map<String, Object> attachmentValue = new LinkedHashMap<>();
            attachmentValue.put(CrmContractConstants.ATTACHMENT_PROP_PATH, record.getAttachmentUrl());
            attachmentValue.put(CrmContractConstants.ATTACHMENT_PROP_NAME, record.getAttachmentFileName());
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put(CrmContractConstants.CONTRACT_FIELD_ID, crmContractId);
            fields.put(crmContractProperties.getAttachmentFieldKey(), List.of(attachmentValue));
            CrmContractEditRequest request = CrmContractEditRequest.builder()
                    .dataId(crmContractId)
                    .fields(fields)
                    .isTrigger(false)
                    .checkRule(false)
                    .build();

            // 4. 调用 CRM 修改销售合同接口
            CrmOpenApiResponse response = crmClient.contractEdit(request);
            if (response.isSuccess()) {
                String message = "CRM回传成功, returnCode=" + response.getReturnCode()
                        + ", msgId=" + response.getMsgId();
                boolean updated = syncService.markSuccess(id, record.getVersion(), message);
                if (updated) {
                    log.info("【{}】CRM回传成功, id={}, oaFormMainId={}, ncOrderNo={}, crmContractId={}, msgId={}",
                            BIZ, id, record.getOaFormMainId(), ncOrderNo,
                            crmContractId, response.getMsgId());
                    result.recordSuccess(item);
                } else {
                    result.recordSkip(item, "成功回写未生效，状态或版本已变化");
                }
            } else {
                String message = "CRM回传失败, returnCode=" + response.getReturnCode()
                        + ", returnMsg=" + response.getReturnMsg();
                log.info("【{}】CRM回传终止为重试, id={}, oaFormMainId={}, ncOrderNo={}, crmContractId={}, {}",
                        BIZ, id, record.getOaFormMainId(), ncOrderNo, crmContractId, message);
                syncService.markCrmRetry(id, record.getVersion(), message);
                result.recordFailure(item, message);
            }
        } catch (Exception e) {
            log.warn("【{}】CRM回传单条失败, id={}, oaFormMainId={}, ncOrderNo={}, crmContractId={}",
                    BIZ, id, record.getOaFormMainId(), ncOrderNo, record.getCrmContractId(), e);
            try {
                syncService.markCrmRetry(id, record.getVersion(), e.getMessage());
            } catch (Exception markEx) {
                log.error("【{}】CRM失败状态回写异常, id={}, ncOrderNo={}", BIZ, id, ncOrderNo, markEx);
            }
            result.recordFailure(item, e.getMessage());
        }
    }

    /**
     * CRM 合同数据 ID 存量补偿：按 NC 销售订单号反查 OA formmain_2817.field0495 并回写中间表
     *
     * <p>field0495 标量子查询上线前入库的记录 {@code crm_contract_id} 为空，且 NOT IN 增量
     * 扫描不会重刷已入库记录，故在处理链路反查补偿；反查异常按未命中处理（不阻断批次内其他记录）。
     * 补偿回写仅补快照字段，不累加 version、不改 sync_status。</p>
     *
     * @param record  当前处理记录
     * @param version 当前乐观锁版本号，仅用于日志观测
     * @return 补偿到的 CRM 合同数据 ID；反查未命中或异常时返回 null
     */
    private String compensateCrmContractId(OaContractAttachmentSyncDO record, int version) {
        try {
            Optional<String> resolved = oaContractDbClient.findCrmContractIdByNcNo(record.getNcOrderNo());
            if (resolved.isEmpty() || !StringUtils.hasText(resolved.get())) {
                log.info("【{}】CRM合同ID反查未命中, id={}, oaFormMainId={}, ncOrderNo={}, version={}",
                        BIZ, record.getId(), record.getOaFormMainId(), record.getNcOrderNo(), version);
                return null;
            }
            syncService.updateCrmContractId(record.getId(), resolved.get());
            log.info("【{}】CRM合同ID补偿成功, id={}, oaFormMainId={}, ncOrderNo={}, crmContractId={}, version={}",
                    BIZ, record.getId(), record.getOaFormMainId(), record.getNcOrderNo(), resolved.get(), version);
            return resolved.get();
        } catch (Exception e) {
            log.warn("【{}】CRM合同ID补偿异常, id={}, ncOrderNo={}",
                    BIZ, record.getId(), record.getNcOrderNo(), e);
            return null;
        }
    }
}
