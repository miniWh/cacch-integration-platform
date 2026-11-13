package com.cacch.integration.manager.mdm.api.impl;

import com.cacch.integration.common.constant.mdm.MdmDeptCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmDeptLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.enums.mdm.MdmDeptIngestResultEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.manager.mdm.api.IMdmDeptSyncManager;
import com.cacch.integration.service.ihr.api.IIhrOrgService;
import com.cacch.integration.service.mdm.api.IMdmDeptIngestService;
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
 * 部门主数据（MDM）iHR 部门同步 Manager 实现
 *
 * <p>fullSync 编排（含 HTTP 不持事务）：分页全量拉取 → 逐部门 ingestOne
 * （单部门事务，逐条隔离失败）→ 停用比对（双守卫）→ ods/mdm 一致性核对。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmDeptSyncManagerImpl implements IMdmDeptSyncManager {

    private static final String BIZ = MdmDeptLogConstants.LOG_BIZ;

    /**
     * 静态 ObjectMapper —— 仅用于 stg 贴源 rawJson 重建
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    /**
     * 全量同步互斥标志（防重入）
     */
    private final AtomicBoolean running = new AtomicBoolean();

    private final IIhrOrgService ihrOrgService;
    private final IMdmDeptIngestService mdmDeptIngestService;

    @Override
    public DeptSyncResult fullSync() {
        if (!running.compareAndSet(false, true)) {
            log.info("【{}】全量触发被拒绝, reason=上一轮同步仍在执行中", BIZ);
            throw new BizException(ResultCode.SYSTEM_ERROR, "上一轮部门全量同步仍在执行中，请勿重复触发");
        }
        try {
            return doFullSync();
        } finally {
            running.set(false);
        }
    }

    /**
     * 全量同步主体：拉取 → 逐部门落库 → 停用比对（守卫）→ 一致性核对
     */
    private DeptSyncResult doFullSync() {
        String etlBatchNo = MdmDeptCommonConstants.buildEtlBatchNo();
        log.info("【{}】开始本轮 iHR 部门全量同步, batchNo={}", BIZ, etlBatchNo);

        List<IhrDepartment> depts = ihrOrgService.fetchAllDepartments();
        BatchProcessResult<String> result = new BatchProcessResult<>();
        if (CollectionUtils.isEmpty(depts)) {
            log.info("【{}】部门清单为空, 本轮结束（跳过停用比对与核对）, batchNo={}", BIZ, etlBatchNo);
            return new DeptSyncResult(result, 0, null);
        }

        Set<String> aliveUuids = new HashSet<>();
        for (IhrDepartment dept : depts) {
            String deptUuid = dept.getUuid();
            if (!StringUtils.hasText(deptUuid)) {
                log.info("【{}】部门 uuid 缺失, 跳过该部门, name={}, batchNo={}", BIZ, dept.getName(), etlBatchNo);
                result.recordSkip("uuid-missing", "部门 uuid 缺失");
                continue;
            }
            aliveUuids.add(deptUuid);
            try {
                // 搜索接口无 per-dept 原文，重建单部门 JSON 作贴源留底
                String rawJson = MAPPER.writeValueAsString(dept);
                MdmDeptIngestResultEnum ingestResult = mdmDeptIngestService.ingestOne(dept, rawJson, etlBatchNo);
                switch (ingestResult) {
                    case INGESTED -> result.recordSuccess(deptUuid);
                    case SKIPPED_UNCHANGED -> result.recordSkip(deptUuid, ingestResult.getDesc());
                    default -> {
                        log.info("【{}】未知落库结果按跳过处理, ihrDeptUuid={}, result={}", BIZ, deptUuid, ingestResult);
                        result.recordSkip(deptUuid, ingestResult.getDesc());
                    }
                }
            } catch (Exception e) {
                log.info("【{}】单部门处理终止, ihrDeptUuid={}, batchNo={}, reason={}",
                        BIZ, deptUuid, etlBatchNo, e.getMessage());
                log.error("【{}】单部门落库失败, ihrDeptUuid={}", BIZ, deptUuid, e);
                result.recordFailure(deptUuid, e.getMessage());
            }
        }

        // 停用比对（守卫：本轮存在拉取/落库失败时跳过，防个别部门失败被误判为已删除）
        int disabledCount = 0;
        if (result.getFailureCount() > 0) {
            log.info("【{}】本轮存在失败部门, 跳过停用比对, reason=防误停用, 失败数={}, batchNo={}",
                    BIZ, result.getFailureCount(), etlBatchNo);
        } else {
            disabledCount = mdmDeptIngestService.disableMissing(aliveUuids, etlBatchNo);
        }

        // 一致性核对
        IMdmDeptIngestService.DeptVerifyResult verify = mdmDeptIngestService.verifyWithOds();
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
        return new DeptSyncResult(result, disabledCount, verify);
    }
}
