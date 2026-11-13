package com.cacch.integration.manager.mdm.api.impl;

import com.cacch.integration.common.constant.ihr.IhrStaffConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.enums.mdm.MdmEmpIngestResultEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.mdm.StgStaffRecordDO;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffModificationInfo;
import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager;
import com.cacch.integration.service.ihr.api.IIhrStaffService;
import com.cacch.integration.service.mdm.api.IMdmEmpIncrementalQueueService;
import com.cacch.integration.service.mdm.api.IMdmEmpIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 人员主数据（MDM）iHR 人员同步 Manager 实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmEmpSyncManagerImpl implements IMdmEmpSyncManager {

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;
    private static final String SOURCE_ADD = "ADD";
    private static final String SOURCE_UPDATE = "UPDATE";

    /**
     * 全量同步互斥标志（Pull/PullIncremental/ProcessIncremental 各自独立锁）
     */
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean pullIncrementalRunning = new AtomicBoolean();
    private final AtomicBoolean processIncrementalRunning = new AtomicBoolean();

    private final IIhrStaffService ihrStaffService;
    private final IMdmEmpIngestService mdmEmpIngestService;
    private final IMdmEmpIncrementalQueueService incrementalQueueService;

    @Override
    public BatchProcessResult<String> pullAndIngest() {
        if (!running.compareAndSet(false, true)) {
            log.info("【{}】全量触发被拒绝, reason=上一轮同步仍在执行中", BIZ);
            throw new BizException(ResultCode.SYSTEM_ERROR, "上一轮全量同步仍在执行中，请勿重复触发");
        }
        try {
            return doPullAndIngest();
        } finally {
            running.set(false);
        }
    }

    @Override
    public PullIncrementalResult pullIncrementalToQueue() {
        if (!pullIncrementalRunning.compareAndSet(false, true)) {
            log.info("【{}】Pull增量触发被拒绝, reason=上一轮Pull仍在执行中", BIZ);
            throw new BizException(ResultCode.SYSTEM_ERROR, "上一轮增量 Pull 仍在执行中，请勿重复触发");
        }
        try {
            return doPullIncrementalToQueue();
        } finally {
            pullIncrementalRunning.set(false);
        }
    }

    @Override
    public BatchProcessResult<String> processIncrementalFromQueue(int limit) {
        if (!processIncrementalRunning.compareAndSet(false, true)) {
            log.info("【{}】Process增量触发被拒绝, reason=上一轮Process仍在执行中", BIZ);
            throw new BizException(ResultCode.SYSTEM_ERROR, "上一轮增量 Process 仍在执行中，请勿重复触发");
        }
        try {
            return doProcessIncrementalFromQueue(limit);
        } finally {
            processIncrementalRunning.set(false);
        }
    }

    // ==================== 全量落库（原逻辑不动） ====================

    private BatchProcessResult<String> doPullAndIngest() {
        String etlBatchNo = MdmEmpCommonConstants.buildEtlBatchNo();
        log.info("【{}】开始本轮 iHR 人员拉取落库, batchNo={}", BIZ, etlBatchNo);

        List<String> staffIds = ihrStaffService.fetchAllStaffIds(null);
        BatchProcessResult<String> result = new BatchProcessResult<>();
        if (CollectionUtils.isEmpty(staffIds)) {
            log.info("【{}】员工 ID 清单为空, 本轮结束, batchNo={}", BIZ, etlBatchNo);
            return result;
        }

        for (String staffId : staffIds) {
            try {
                IhrStaffBasicResponse resp = ihrStaffService.fetchStaffBasicResponse(staffId);
                if (resp.getData() == null) {
                    log.info("【{}】响应无 data, 跳过该员工, ihrStaffId={}, batchNo={}", BIZ, staffId, etlBatchNo);
                    result.recordSkip(staffId, "响应无 data");
                    continue;
                }
                MdmEmpIngestResultEnum ingestResult = mdmEmpIngestService.ingestOne(
                        resp.getData(), resp.getRawJson(), etlBatchNo);
                switch (ingestResult) {
                    case INGESTED -> result.recordSuccess(staffId);
                    case SKIPPED_UNCHANGED, SKIPPED_MISSING_IDCARD ->
                            result.recordSkip(staffId, ingestResult.getDesc());
                    default -> {
                        log.info("【{}】未知落库结果按跳过处理, ihrStaffId={}, result={}", BIZ, staffId, ingestResult);
                        result.recordSkip(staffId, ingestResult.getDesc());
                    }
                }
            } catch (Exception e) {
                log.info("【{}】单员工处理终止, ihrStaffId={}, batchNo={}, reason={}",
                        BIZ, staffId, etlBatchNo, e.getMessage());
                log.error("【{}】单员工落库失败, ihrStaffId={}", BIZ, staffId, e);
                result.recordFailure(staffId, e.getMessage());
            }
        }

        log.info("【{}】本轮拉取落库完成, batchNo={}, 总数={}, 成功={}, 跳过={}, 失败={}",
                BIZ, etlBatchNo, result.getTotalCount(), result.getSuccessCount(),
                result.getSkipCount(), result.getFailureCount());
        return result;
    }

    // ==================== Pull 增量入队 ====================

    /**
     * 并发拉取 additions / modifications IN_SERVICE / modifications QUIT（各 T + T-1）
     * → insertIgnore DO UPDATE 入队 stg_staff_record
     */
    private PullIncrementalResult doPullIncrementalToQueue() {
        String pullBatchNo = MdmEmpCommonConstants.buildPullBatchNo();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        log.info("【{}】开始本轮增量 Pull 入队, pullBatchNo={}, dates=[{}, {}]",
                BIZ, pullBatchNo, today, yesterday);

        AtomicInteger addCallCount = new AtomicInteger();
        AtomicInteger updateCallCount = new AtomicInteger();

        // CompletableFuture 并发 3 组接口（共 6 次 HTTP 调用）
        CompletableFuture<Void> cfAdd = CompletableFuture.allOf(
                buildAdditionsFuture(today, pullBatchNo, addCallCount),
                buildAdditionsFuture(yesterday, pullBatchNo, addCallCount)
        );

        CompletableFuture<Void> cfUpdIn = CompletableFuture.allOf(
                buildModificationsFuture(today, IhrStaffConstants.STAFF_STATUS_IN_SERVICE, pullBatchNo, updateCallCount),
                buildModificationsFuture(yesterday, IhrStaffConstants.STAFF_STATUS_IN_SERVICE, pullBatchNo, updateCallCount)
        );

        CompletableFuture<Void> cfUpdQuit = CompletableFuture.allOf(
                buildModificationsFuture(today, IhrStaffConstants.STAFF_STATUS_QUIT, pullBatchNo, updateCallCount),
                buildModificationsFuture(yesterday, IhrStaffConstants.STAFF_STATUS_QUIT, pullBatchNo, updateCallCount)
        );

        CompletableFuture.allOf(cfAdd, cfUpdIn, cfUpdQuit).join();

        int totalCalls = addCallCount.get() + updateCallCount.get();
        log.info("【{}】Pull 入队完成, pullBatchNo={}, iHR HTTP调用次数={}, 预计入队行>={}",
                BIZ, pullBatchNo, totalCalls, totalCalls);
        return new PullIncrementalResult(addCallCount.get(), 0, updateCallCount.get(), 0, pullBatchNo);
    }

    /**
     * 拉取指定日期 additions 接口 → 逐条 insertIgnore 入队 source=ADD
     *
     * <p>Do NOT wrap exceptions — 上层 CompletableFuture 会感知任一接口失败并终止整轮 Pull。</p>
     */
    private CompletableFuture<Void> buildAdditionsFuture(LocalDate date, String pullBatchNo, AtomicInteger callCounter) {
        return CompletableFuture.runAsync(() -> {
            List<String> staffIds = ihrStaffService.fetchAddedStaffIds(date);
            if (CollectionUtils.isEmpty(staffIds)) {
                log.info("【{}】additions 返回空, date={}, 跳过入队", BIZ, date);
                return;
            }
            int enqueued = 0;
            for (String staffId : staffIds) {
                incrementalQueueService.enqueueOne(staffId, SOURCE_ADD, 0L, pullBatchNo);
                enqueued++;
            }
            callCounter.incrementAndGet();
            log.info("【{}】additions 入队完成, date={}, 入队={}", BIZ, date, enqueued);
        });
    }

    /**
     * 拉取指定日期 modifications V2 接口 → 逐条 insertIgnore 入队 source=UPDATE
     */
    private CompletableFuture<Void> buildModificationsFuture(LocalDate date, String staffStatus,
                                                            String pullBatchNo, AtomicInteger callCounter) {
        return CompletableFuture.runAsync(() -> {
            List<IhrStaffModificationInfo> mods = ihrStaffService.fetchModifications(date, staffStatus);
            if (CollectionUtils.isEmpty(mods)) {
                log.info("【{}】modifications 返回空, date={}, staffStatus={}, 跳过入队", BIZ, date, staffStatus);
                return;
            }
            int enqueued = 0;
            for (IhrStaffModificationInfo mod : mods) {
                long lastUpdate = mod.getLastUpdate() != null ? mod.getLastUpdate() : 0L;
                incrementalQueueService.enqueueOne(mod.getStaffId(), SOURCE_UPDATE, lastUpdate, pullBatchNo);
                enqueued++;
            }
            callCounter.incrementAndGet();
            log.info("【{}】modifications 入队完成, date={}, staffStatus={}, 入队={}",
                    BIZ, date, staffStatus, enqueued);
        });
    }

    // ==================== Process 增量落库 ====================

    /**
     * 从 stg_staff_record 认领一批待处理行 → 调 fetchStaffBasic → ingestOne → 立即 markProcessed
     *
     * <p>UPDATE…RETURNING 原子认领（行置 PROCESSING）防多实例并发；
     * 认领与处理分离，逐个处理（每个 ingestOne 自带 REQUIRED 事务），
     * 处理完成标 PROCESSED，异常崩溃的行由超时回收重置 PENDING。</p>
     */
    private BatchProcessResult<String> doProcessIncrementalFromQueue(int limit) {
        String processBatchNo = MdmEmpCommonConstants.buildProcessBatchNo();
        String etlBatchNo = MdmEmpCommonConstants.buildEtlBatchNo();
        log.info("【{}】开始本轮增量 Process, processBatchNo={}, etlBatchNo={}, limit={}",
                BIZ, processBatchNo, etlBatchNo, limit);

        // 原子认领：单条 SQL 置 PROCESSING，并发（含多实例）互斥
        List<StgStaffRecordDO> batch = incrementalQueueService.claimPendingBatch(limit);
        if (CollectionUtils.isEmpty(batch)) {
            log.info("【{}】PENDING 队列为空, 本轮 Process 结束", BIZ);
            return new BatchProcessResult<>();
        }
        log.info("【{}】已认领 {} 行待处理（ADD先 / UPDATE按lastUpdate升序）", BIZ, batch.size());

        BatchProcessResult<String> result = new BatchProcessResult<>();
        for (StgStaffRecordDO row : batch) {
            try {
                IhrStaffBasicResponse resp = ihrStaffService.fetchStaffBasicResponse(row.getStaffId());
                if (resp.getData() == null) {
                    log.info("【{}】响应无 data, 跳过该行, staffId={}, processBatchNo={}",
                            BIZ, row.getStaffId(), processBatchNo);
                    result.recordSkip(row.getStaffId(), "iHR 响应无 data");
                    continue;
                }
                MdmEmpIngestResultEnum ingestResult = mdmEmpIngestService.ingestOne(
                        resp.getData(), resp.getRawJson(), etlBatchNo);
                switch (ingestResult) {
                    case INGESTED -> result.recordSuccess(row.getStaffId());
                    case SKIPPED_UNCHANGED, SKIPPED_MISSING_IDCARD ->
                            result.recordSkip(row.getStaffId(), ingestResult.getDesc());
                    default -> {
                        log.info("【{}】未知落库结果按跳过处理, staffId={}, result={}",
                                BIZ, row.getStaffId(), ingestResult);
                        result.recordSkip(row.getStaffId(), ingestResult.getDesc());
                    }
                }
            } catch (Exception e) {
                log.info("【{}】单员工处理终止, staffId={}, processBatchNo={}, reason={}",
                        BIZ, row.getStaffId(), processBatchNo, e.getMessage());
                log.error("【{}】单员工增量落库失败, staffId={}", BIZ, row.getStaffId(), e);
                result.recordFailure(row.getStaffId(), e.getMessage());
            } finally {
                // 无论成功/跳过/失败，都标记 PROCESSED
                incrementalQueueService.markOneProcessed(row.getId(), processBatchNo);
            }
        }

        log.info("【{}】本轮增量 Process 完成, processBatchNo={}, 总数={}, 成功={}, 跳过={}, 失败={}",
                BIZ, processBatchNo, result.getTotalCount(), result.getSuccessCount(),
                result.getSkipCount(), result.getFailureCount());
        return result;
    }
}
