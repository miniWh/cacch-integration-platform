package com.cacch.integration.manager.mdm.emp.incremental.support;

import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.common.enums.mdm.MdmIngestResultEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 人员单条落库结果写入批量统计
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
public final class MdmEmpIngestResultRecorder {

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;

    private MdmEmpIngestResultRecorder() {
    }

    /**
     * 按落库结果分类记录批量统计
     *
     * @param result       批量统计结果
     * @param staffId      员工 ID
     * @param ingestResult 单员工落库结果
     */
    public static void record(BatchProcessResult<String> result, String staffId,
                              MdmIngestResultEnum ingestResult) {
        switch (ingestResult) {
            case INGESTED -> result.recordSuccess(staffId);
            case SKIPPED_UNCHANGED, SKIPPED_MISSING_IDCARD -> result.recordSkip(staffId, ingestResult.getDesc());
            default -> {
                log.info("【{}】未知落库结果按跳过处理, ihrStaffId={}, result={}", BIZ, staffId, ingestResult);
                result.recordSkip(staffId, ingestResult.getDesc());
            }
        }
    }
}