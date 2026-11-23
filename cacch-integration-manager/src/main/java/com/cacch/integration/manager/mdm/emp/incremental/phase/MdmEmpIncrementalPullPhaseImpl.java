package com.cacch.integration.manager.mdm.emp.incremental.phase;

import com.cacch.integration.common.constant.ihr.IhrStaffConstants;
import com.cacch.integration.common.constant.mdm.MdmCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.enums.mdm.MdmEmpIncrementalSyncPhaseStatus;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffModificationInfo;
import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager.PullIncrementalResult;
import com.cacch.integration.manager.mdm.emp.incremental.api.IMdmEmpIncrementalPullPhase;
import com.cacch.integration.manager.mdm.emp.incremental.model.IncrementalPullPhaseResult;
import com.cacch.integration.manager.mdm.emp.incremental.model.PullRunMode;
import com.cacch.integration.manager.mdm.emp.incremental.support.MdmEmpIncrementalSyncAlerter;
import com.cacch.integration.manager.mdm.emp.incremental.support.MdmEmpSyncExecutionGuard;
import com.cacch.integration.service.ihr.api.IIhrStaffService;
import com.cacch.integration.service.mdm.api.IMdmEmpIncrementalQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 人员增量 Pull 阶段实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmEmpIncrementalPullPhaseImpl implements IMdmEmpIncrementalPullPhase {

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;
    private static final String SOURCE_ADD = "ADD";
    private static final String SOURCE_UPDATE = "UPDATE";
    private static final String MUTEX_REJECT_REASON = "上一轮增量 Pull 仍在执行中，请勿重复触发";

    private final MdmEmpSyncExecutionGuard executionGuard;
    private final MdmEmpIncrementalSyncAlerter alerter;
    private final IIhrStaffService ihrStaffService;
    private final IMdmEmpIncrementalQueueService incrementalQueueService;

    @Override
    public IncrementalPullPhaseResult run(PullRunMode mode) {
        if (!executionGuard.tryAcquirePull()) {
            log.info("【{}】Pull增量触发被拒绝, reason={}", BIZ, MUTEX_REJECT_REASON);
            alerter.alertPullStage(MUTEX_REJECT_REASON, null);
            IncrementalPullPhaseResult outcome = IncrementalPullPhaseResult.rejectedMutex(MUTEX_REJECT_REASON);
            return failFastIfNeeded(mode, outcome);
        }
        try {
            PullIncrementalResult pullResult = executePullBody();
            return IncrementalPullPhaseResult.success(pullResult);
        } catch (Exception e) {
            alerter.alertPullStage(e.getMessage(), e);
            IncrementalPullPhaseResult outcome = IncrementalPullPhaseResult.failed(e.getMessage());
            return failFastIfNeeded(mode, outcome);
        } finally {
            executionGuard.releasePull();
        }
    }

    private IncrementalPullPhaseResult failFastIfNeeded(PullRunMode mode, IncrementalPullPhaseResult outcome) {
        if (mode == PullRunMode.FAIL_FAST && outcome.status() != MdmEmpIncrementalSyncPhaseStatus.SUCCESS) {
            throw new BizException(ResultCode.SYSTEM_ERROR, outcome.errorMessage());
        }
        return outcome;
    }

    private PullIncrementalResult executePullBody() {
        String pullBatchNo = MdmCommonConstants.buildPullBatchNo();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        log.info("【{}】开始本轮增量 Pull 入队, pullBatchNo={}, dates=[{}, {}]",
                BIZ, pullBatchNo, today, yesterday);

        AtomicInteger addCallCount = new AtomicInteger();
        AtomicInteger updateCallCount = new AtomicInteger();

        CompletableFuture<Void> cfAdd = CompletableFuture.allOf(
                runAdditionsAsync(today, pullBatchNo, addCallCount),
                runAdditionsAsync(yesterday, pullBatchNo, addCallCount)
        );
        CompletableFuture<Void> cfUpdIn = CompletableFuture.allOf(
                runModificationsAsync(today, IhrStaffConstants.STAFF_STATUS_IN_SERVICE, pullBatchNo, updateCallCount),
                runModificationsAsync(yesterday, IhrStaffConstants.STAFF_STATUS_IN_SERVICE, pullBatchNo, updateCallCount)
        );
        CompletableFuture<Void> cfUpdQuit = CompletableFuture.allOf(
                runModificationsAsync(today, IhrStaffConstants.STAFF_STATUS_QUIT, pullBatchNo, updateCallCount),
                runModificationsAsync(yesterday, IhrStaffConstants.STAFF_STATUS_QUIT, pullBatchNo, updateCallCount)
        );

        CompletableFuture.allOf(cfAdd, cfUpdIn, cfUpdQuit).join();

        int totalCalls = addCallCount.get() + updateCallCount.get();
        log.info("【{}】Pull 入队完成, pullBatchNo={}, iHR HTTP调用次数={}, 预计入队行>={}",
                BIZ, pullBatchNo, totalCalls, totalCalls);
        return new PullIncrementalResult(addCallCount.get(), 0, updateCallCount.get(), 0, pullBatchNo);
    }

    private CompletableFuture<Void> runAdditionsAsync(LocalDate date, String pullBatchNo, AtomicInteger callCounter) {
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

    private CompletableFuture<Void> runModificationsAsync(LocalDate date, String staffStatus,
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
}