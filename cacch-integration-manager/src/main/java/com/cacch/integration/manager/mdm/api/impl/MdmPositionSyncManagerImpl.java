package com.cacch.integration.manager.mdm.api.impl;

import com.cacch.integration.common.constant.mdm.MdmCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmPositionLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.enums.mdm.MdmIngestResultEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.dto.IhrPosition;
import com.cacch.integration.manager.mdm.api.IMdmPositionSyncManager;
import com.cacch.integration.service.ihr.api.IIhrPositionSyncService;
import com.cacch.integration.service.mdm.api.IMdmPositionIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 岗位主数据（MDM）iHR 岗位同步 Manager 实现
 *
 * <p>fullSync 编排（含 HTTP 不持事务）：全量拉取（无分页）→ 逐岗位 ingestOne
 * （单岗位事务，逐条隔离失败；rowHash 未变化跳过）→ 停用比对（双守卫）→ ods/mdm 一致性核对。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmPositionSyncManagerImpl implements IMdmPositionSyncManager {

    private static final String BIZ = MdmPositionLogConstants.LOG_BIZ;

    /**
     * 静态 ObjectMapper —— 仅用于 stg 贴源 rawJson 重建
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    /**
     * 全量同步互斥标志（防重入）
     */
    private final AtomicBoolean running = new AtomicBoolean();

    private final IIhrPositionSyncService ihrPositionSyncService;
    private final IMdmPositionIngestService mdmPositionIngestService;

    @Override
    public PositionSyncResult fullSync() {
        if (!running.compareAndSet(false, true)) {
            log.info("【{}】全量触发被拒绝, reason=上一轮同步仍在执行中", BIZ);
            throw new BizException(ResultCode.SYSTEM_ERROR, "上一轮岗位全量同步仍在执行中，请勿重复触发");
        }
        try {
            return doFullSync();
        } finally {
            running.set(false);
        }
    }

    /**
     * 全量同步主体：拉取 → 逐岗位落库 → 停用比对（守卫）→ 一致性核对
     */
    private PositionSyncResult doFullSync() {
        String etlBatchNo = MdmCommonConstants.buildEtlBatchNo("POS");
        log.info("【{}】开始本轮 iHR 岗位全量同步, batchNo={}", BIZ, etlBatchNo);

        List<IhrPosition> positions = ihrPositionSyncService.fetchAllPositions();
        BatchProcessResult<String> result = new BatchProcessResult<>();
        if (CollectionUtils.isEmpty(positions)) {
            log.info("【{}】岗位清单为空, 本轮结束（跳过停用比对与核对）, batchNo={}", BIZ, etlBatchNo);
            return new PositionSyncResult(result, 0, null);
        }

        Set<String> aliveUuids = new HashSet<>();
        for (IhrPosition position : positions) {
            String positionUuid = position.getId();
            if (!StringUtils.hasText(positionUuid)) {
                log.info("【{}】岗位 uuid 缺失, 跳过该岗位, positionName={}, batchNo={}",
                        BIZ, position.getPositionName(), etlBatchNo);
                result.recordSkip("uuid-missing", "岗位 uuid 缺失");
                continue;
            }
            aliveUuids.add(positionUuid);
            try {
                // 清单接口无 per-position 原文，重建单岗位 JSON 作贴源留底
                String rawJson = MAPPER.writeValueAsString(position);
                MdmIngestResultEnum ingestResult =
                        mdmPositionIngestService.ingestOne(position, rawJson, etlBatchNo);
                switch (ingestResult) {
                    case INGESTED -> result.recordSuccess(positionUuid);
                    case SKIPPED_UNCHANGED -> result.recordSkip(positionUuid, ingestResult.getDesc());
                    default -> {
                        log.info("【{}】未知落库结果按跳过处理, ihrPositionUuid={}, result={}",
                                BIZ, positionUuid, ingestResult);
                        result.recordSkip(positionUuid, ingestResult.getDesc());
                    }
                }
            } catch (Exception e) {
                log.info("【{}】单岗位处理终止, ihrPositionUuid={}, batchNo={}, reason={}",
                        BIZ, positionUuid, etlBatchNo, e.getMessage());
                log.error("【{}】单岗位落库失败, ihrPositionUuid={}", BIZ, positionUuid, e);
                result.recordFailure(positionUuid, e.getMessage());
            }
        }

        // 停用比对（守卫：本轮存在拉取/落库失败时跳过，防个别岗位失败被误判为已删除）
        int disabledCount = 0;
        if (result.getFailureCount() > 0) {
            log.info("【{}】本轮存在失败岗位, 跳过停用比对, reason=防误停用, 失败数={}, batchNo={}",
                    BIZ, result.getFailureCount(), etlBatchNo);
        } else {
            disabledCount = mdmPositionIngestService.disableMissing(aliveUuids, etlBatchNo);
        }

        // 一致性核对
        IMdmPositionIngestService.PositionVerifyResult verify = mdmPositionIngestService.verifyWithOds();
        if (!verify.missingInMdm().isEmpty() || !verify.missingInOds().isEmpty()
                || verify.mdmCount() != verify.odsCount()) {
            log.warn("【{}】ods/mdm 一致性核对存在差异, mdmCount={}, odsCount={}, missingInMdm={}, missingInOds={}",
                    BIZ, verify.mdmCount(), verify.odsCount(), verify.missingInMdm(), verify.missingInOds());
        } else {
            log.info("【{}】ods/mdm 一致性核对通过, mdmCount={}, odsCount={}", BIZ, verify.mdmCount(), verify.odsCount());
        }

        log.info("【{}】本轮全量同步完成, batchNo={}, 总数={}, 成功={}, 跳过={}, 失败={}, 停用数={}",
                BIZ, etlBatchNo, result.getTotalCount(), result.getSuccessCount(),
                result.getSkipCount(), result.getFailureCount(), disabledCount);
        return new PositionSyncResult(result, disabledCount, verify);
    }
}
