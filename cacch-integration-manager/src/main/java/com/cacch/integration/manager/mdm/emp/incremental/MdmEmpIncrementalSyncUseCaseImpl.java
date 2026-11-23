package com.cacch.integration.manager.mdm.emp.incremental;

import com.cacch.integration.common.constant.mdm.MdmEmpLogConstants;
import com.cacch.integration.manager.mdm.api.IMdmEmpSyncManager.IncrementalSyncResult;
import com.cacch.integration.manager.mdm.emp.incremental.api.IMdmEmpIncrementalProcessPhase;
import com.cacch.integration.manager.mdm.emp.incremental.api.IMdmEmpIncrementalPullPhase;
import com.cacch.integration.manager.mdm.emp.incremental.api.IMdmEmpIncrementalSyncUseCase;
import com.cacch.integration.manager.mdm.emp.incremental.model.IncrementalProcessPhaseResult;
import com.cacch.integration.manager.mdm.emp.incremental.model.IncrementalPullPhaseResult;
import com.cacch.integration.manager.mdm.emp.incremental.model.PullRunMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 人员增量同步用例实现 —— Pull（吞异常）→ Process
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmEmpIncrementalSyncUseCaseImpl implements IMdmEmpIncrementalSyncUseCase {

    private static final String BIZ = MdmEmpLogConstants.LOG_BIZ;

    private final IMdmEmpIncrementalPullPhase pullPhase;
    private final IMdmEmpIncrementalProcessPhase processPhase;

    @Override
    public IncrementalSyncResult run(int limit) {
        log.info("【{}】开始增量编排（Pull → Process）, processLimit={}", BIZ, limit);

        IncrementalPullPhaseResult pullOutcome = pullPhase.run(PullRunMode.SWALLOW_FOR_ORCHESTRATION);
        IncrementalProcessPhaseResult processOutcome = processPhase.run(limit);

        String pullBatchNo = pullOutcome.pullResult() != null ? pullOutcome.pullResult().pullBatchNo() : null;
        log.info("【{}】增量编排完成, pullStatus={}, pullBatchNo={}, pullError={}, processStatus={}, "
                        + "process 总数={}, 成功={}, 跳过={}, 失败={}",
                BIZ, pullOutcome.status(), pullBatchNo, pullOutcome.errorMessage(), processOutcome.status(),
                processOutcome.batchResult().getTotalCount(), processOutcome.batchResult().getSuccessCount(),
                processOutcome.batchResult().getSkipCount(), processOutcome.batchResult().getFailureCount());

        return new IncrementalSyncResult(
                pullOutcome.status(),
                pullOutcome.pullResult(),
                pullOutcome.errorMessage(),
                processOutcome.status(),
                processOutcome.batchResult(),
                null
        );
    }
}