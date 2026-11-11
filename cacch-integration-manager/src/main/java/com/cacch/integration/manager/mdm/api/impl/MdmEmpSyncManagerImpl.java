package com.cacch.integration.manager.mdm.api.impl;

import com.cacch.integration.common.constant.mdm.MdmEmpCommonConstants;
import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.enums.mdm.MdmEmpIngestResultEnum;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicResponse;
import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager;
import com.cacch.integration.service.ihr.api.IIhrStaffService;
import com.cacch.integration.service.mdm.api.IMdmEmpIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /**
     * 同步互斥标志 —— 上一轮未结束时拒绝新请求（进程内单实例语义）
     */
    private final AtomicBoolean running = new AtomicBoolean();

    private final IIhrStaffService ihrStaffService;
    private final IMdmEmpIngestService mdmEmpIngestService;

    @Override
    public BatchProcessResult<String> pullAndIngest() {
        if (!running.compareAndSet(false, true)) {
            log.info("【{}】触发被拒绝, reason=上一轮同步仍在执行中", BIZ);
            throw new BizException(ResultCode.SYSTEM_ERROR, "上一轮同步仍在执行中，请勿重复触发");
        }
        try {
            return doPullAndIngest();
        } finally {
            running.set(false);
        }
    }

    /**
     * 实际拉取落库逻辑（与互斥锁分离，便于后续替换锁实现）
     *
     * @return 本轮批量处理结果
     */
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
}
