package com.cacch.integration.manager.mdm.api.impl;

import com.cacch.integration.common.constant.ihr.IhrStaffConstants;
import com.cacch.integration.common.constant.mdm.MdmDeptCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.enums.mdm.MdmDeptIngestResultEnum;
import com.cacch.integration.common.enums.mdm.MdmEmpIngestResultEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.mdm.StgStaffRecordDO;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffModificationInfo;
import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager;
import com.cacch.integration.service.ihr.api.IIhrOrgService;
import com.cacch.integration.service.ihr.api.IIhrStaffService;
import com.cacch.integration.service.mdm.api.IMdmDeptIngestService;
import com.cacch.integration.service.mdm.api.IMdmEmpIncrementalQueueService;
import com.cacch.integration.service.mdm.api.IMdmEmpIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final IIhrOrgService ihrOrgService;
    private final IMdmDeptIngestService mdmDeptIngestService;

    /**
     * 静态 ObjectMapper —— 仅用于部门保障阶段的单部门 rawJson 重建
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

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

    // ==================== 全量落库（三阶段：拉取 → 部门保障 → 员工落库） ====================

    private BatchProcessResult<String> doPullAndIngest() {
        String etlBatchNo = MdmEmpCommonConstants.buildEtlBatchNo();
        log.info("【{}】开始本轮 iHR 人员拉取落库, batchNo={}", BIZ, etlBatchNo);

        List<String> staffIds = ihrStaffService.fetchAllStaffIds(null);
        BatchProcessResult<String> result = new BatchProcessResult<>();
        if (CollectionUtils.isEmpty(staffIds)) {
            log.info("【{}】员工 ID 清单为空, 本轮结束, batchNo={}", BIZ, etlBatchNo);
            return result;
        }

        // 阶段A：逐员工拉取（单员工失败即记失败，不参与部门保障与落库）
        Map<String, IhrStaffBasicResponse> responses = new LinkedHashMap<>();
        for (String staffId : staffIds) {
            try {
                responses.put(staffId, ihrStaffService.fetchStaffBasicResponse(staffId));
            } catch (Exception e) {
                log.info("【{}】单员工拉取终止, ihrStaffId={}, batchNo={}, reason={}",
                        BIZ, staffId, etlBatchNo, e.getMessage());
                log.error("【{}】单员工拉取失败, ihrStaffId={}", BIZ, staffId, e);
                result.recordFailure(staffId, e.getMessage());
            }
        }

        // 阶段B：部门保障（变更前 + 变更后部门批量回源落库，失败降级不阻断员工落库）
        ensureDeptsBeforeIngest(responses, etlBatchNo);

        // 阶段C：逐员工落库（单员工事务粒度）
        for (Map.Entry<String, IhrStaffBasicResponse> entry : responses.entrySet()) {
            String staffId = entry.getKey();
            try {
                IhrStaffBasicResponse resp = entry.getValue();
                if (resp.getData() == null) {
                    log.info("【{}】响应无 data, 跳过该员工, ihrStaffId={}, batchNo={}", BIZ, staffId, etlBatchNo);
                    result.recordSkip(staffId, "响应无 data");
                    continue;
                }
                recordIngestResult(result, staffId,
                        mdmEmpIngestService.ingestOne(resp.getData(), resp.getRawJson(), etlBatchNo));
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

    /**
     * 按落库结果分类记录批量统计
     *
     * @param result       批量统计结果
     * @param staffId      员工 ID（明细主体）
     * @param ingestResult 单员工落库结果
     */
    private void recordIngestResult(BatchProcessResult<String> result, String staffId,
                                    MdmEmpIngestResultEnum ingestResult) {
        switch (ingestResult) {
            case INGESTED -> result.recordSuccess(staffId);
            case SKIPPED_UNCHANGED, SKIPPED_MISSING_IDCARD ->
                    result.recordSkip(staffId, ingestResult.getDesc());
            default -> {
                log.info("【{}】未知落库结果按跳过处理, ihrStaffId={}, result={}", BIZ, staffId, ingestResult);
                result.recordSkip(staffId, ingestResult.getDesc());
            }
        }
    }

    /**
     * 员工落库前部门保障 —— 汇总本轮涉及部门（变更后 + 变更前）批量回源并走部门全链路落库
     *
     * <p>汇总规则：各员工当前报文 departmentId（变更后）∪ 上一快照 ods_ihr_emp.ihr_dept_id
     * （变更前，新员工无快照自然不含），去重后经 listDepartmentsByIds（100/批）回源，
     * 逐部门重建 JSON 走 {@code IMdmDeptIngestService.ingestOne}（贴源/ods/mdm/事件全链路）。</p>
     *
     * <p>降级策略：整体回源失败或单部门落库失败均不阻断员工落库
     * （员工 org_code 回退部门 ID 并有 INFO 日志）。</p>
     *
     * @param responses   本轮已拉取的员工响应（staffId → 响应）
     * @param empEtlBatchNo 员工轮次 ETL 批次号（日志关联用）
     */
    private void ensureDeptsBeforeIngest(Map<String, IhrStaffBasicResponse> responses, String empEtlBatchNo) {
        if (responses.isEmpty()) {
            log.info("【{}】部门保障跳过, reason=本轮无员工响应, empBatchNo={}", BIZ, empEtlBatchNo);
            return;
        }

        Set<Long> deptIds = new LinkedHashSet<>();
        for (IhrStaffBasicResponse resp : responses.values()) {
            if (resp.getData() != null && resp.getData().getDepartmentId() != null) {
                deptIds.add(resp.getData().getDepartmentId());
            }
        }
        Map<String, String> prevDeptIds = mdmEmpIngestService.findPrevDeptIdByStaffIds(responses.keySet());
        for (String deptId : prevDeptIds.values()) {
            Long parsed = parseDeptIdSafe(deptId);
            if (parsed != null) {
                deptIds.add(parsed);
            }
        }
        if (deptIds.isEmpty()) {
            log.info("【{}】部门保障跳过, reason=本轮涉及部门为空, empBatchNo={}", BIZ, empEtlBatchNo);
            return;
        }

        String deptEtlBatchNo = MdmDeptCommonConstants.buildEtlBatchNo();
        log.info("【{}】员工落库前部门保障开始, 涉及部门数={}, empBatchNo={}, deptBatchNo={}",
                BIZ, deptIds.size(), empEtlBatchNo, deptEtlBatchNo);
        try {
            List<IhrDepartment> depts = ihrOrgService.listDepartmentsByIds(new ArrayList<>(deptIds));
            if (CollectionUtils.isEmpty(depts)) {
                log.info("【{}】部门保障终止, reason=iHR 未返回任何部门, deptBatchNo={}", BIZ, deptEtlBatchNo);
                return;
            }
            BatchProcessResult<String> deptResult = new BatchProcessResult<>();
            for (IhrDepartment dept : depts) {
                try {
                    String rawJson = MAPPER.writeValueAsString(dept);
                    MdmDeptIngestResultEnum ingestResult =
                            mdmDeptIngestService.ingestOne(dept, rawJson, deptEtlBatchNo);
                    if (ingestResult == MdmDeptIngestResultEnum.INGESTED) {
                        deptResult.recordSuccess(dept.getUuid());
                    } else {
                        deptResult.recordSkip(dept.getUuid(), ingestResult.getDesc());
                    }
                } catch (Exception e) {
                    log.info("【{}】部门保障单部门处理终止, ihrDeptUuid={}, deptBatchNo={}, reason={}",
                            BIZ, dept.getUuid(), deptEtlBatchNo, e.getMessage());
                    log.error("【{}】部门保障单部门落库失败, ihrDeptUuid={}", BIZ, dept.getUuid(), e);
                    deptResult.recordFailure(dept.getUuid(), e.getMessage());
                }
            }
            log.info("【{}】员工落库前部门保障完成, 涉及部门数={}, iHR返回={}, 已落库={}, 无变化跳过={}, 失败={}, deptBatchNo={}",
                    BIZ, deptIds.size(), depts.size(), deptResult.getSuccessCount(),
                    deptResult.getSkipCount(), deptResult.getFailureCount(), deptEtlBatchNo);
        } catch (Exception e) {
            log.info("【{}】员工落库前部门保障终止, 员工落库继续, empBatchNo={}, reason={}",
                    BIZ, empEtlBatchNo, e.getMessage());
            log.error("【{}】员工落库前部门保障失败, empBatchNo={}", BIZ, empEtlBatchNo, e);
        }
    }

    /**
     * 上一快照部门 ID 字符串安全转 Long（非数字脏数据跳过并打 INFO）
     *
     * @param deptId 部门 ID 字符串形式
     * @return Long 值；非数字时返回 null
     */
    private Long parseDeptIdSafe(String deptId) {
        try {
            return Long.parseLong(deptId);
        } catch (NumberFormatException e) {
            log.info("【{}】上一快照部门ID非数字, 跳过该部门保障, ihrDeptId={}", BIZ, deptId);
            return null;
        }
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
     * 从 stg_staff_record 认领一批待处理行，三阶段处理：
     * 逐行拉取（阶段A）→ 部门保障（阶段B）→ 逐行落库并标 PROCESSED（阶段C）
     *
     * <p>UPDATE…RETURNING 原子认领（行置 PROCESSING）防多实例并发；
     * 阶段A 拉取失败的行立即标 PROCESSED；阶段C 落库完成立即标 PROCESSED，
     * 异常崩溃的行由超时回收重置 PENDING。同 staffId 重复行共用一次拉取结果。</p>
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

        // 阶段A：逐行拉取（失败即记失败并标记已处理，不参与后续阶段）
        Map<String, IhrStaffBasicResponse> responses = new LinkedHashMap<>();
        for (StgStaffRecordDO row : batch) {
            try {
                responses.put(row.getStaffId(), ihrStaffService.fetchStaffBasicResponse(row.getStaffId()));
            } catch (Exception e) {
                log.info("【{}】单员工拉取终止, staffId={}, processBatchNo={}, reason={}",
                        BIZ, row.getStaffId(), processBatchNo, e.getMessage());
                log.error("【{}】单员工拉取失败, staffId={}", BIZ, row.getStaffId(), e);
                result.recordFailure(row.getStaffId(), e.getMessage());
                incrementalQueueService.markOneProcessed(row.getId(), processBatchNo);
            }
        }

        // 阶段B：部门保障（变更前 + 变更后部门批量回源落库，失败降级不阻断）
        ensureDeptsBeforeIngest(responses, etlBatchNo);

        // 阶段C：逐行落库（无论成功/跳过/失败都标记 PROCESSED）
        for (StgStaffRecordDO row : batch) {
            if (!responses.containsKey(row.getStaffId())) {
                // 阶段A 已失败并标记处理
                continue;
            }
            try {
                IhrStaffBasicResponse resp = responses.get(row.getStaffId());
                if (resp.getData() == null) {
                    log.info("【{}】响应无 data, 跳过该行, staffId={}, processBatchNo={}",
                            BIZ, row.getStaffId(), processBatchNo);
                    result.recordSkip(row.getStaffId(), "iHR 响应无 data");
                    continue;
                }
                recordIngestResult(result, row.getStaffId(),
                        mdmEmpIngestService.ingestOne(resp.getData(), resp.getRawJson(), etlBatchNo));
            } catch (Exception e) {
                log.info("【{}】单员工处理终止, staffId={}, processBatchNo={}, reason={}",
                        BIZ, row.getStaffId(), processBatchNo, e.getMessage());
                log.error("【{}】单员工增量落库失败, staffId={}", BIZ, row.getStaffId(), e);
                result.recordFailure(row.getStaffId(), e.getMessage());
            } finally {
                incrementalQueueService.markOneProcessed(row.getId(), processBatchNo);
            }
        }

        log.info("【{}】本轮增量 Process 完成, processBatchNo={}, 总数={}, 成功={}, 跳过={}, 失败={}",
                BIZ, processBatchNo, result.getTotalCount(), result.getSuccessCount(),
                result.getSkipCount(), result.getFailureCount());
        return result;
    }
}
