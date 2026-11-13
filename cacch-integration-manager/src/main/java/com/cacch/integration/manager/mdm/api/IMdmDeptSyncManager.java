package com.cacch.integration.manager.mdm.api;

import com.cacch.integration.common.dto.batch.BatchProcessResult;
import com.cacch.integration.service.mdm.api.IMdmDeptIngestService;

/**
 * 部门主数据（MDM）iHR 部门同步 Manager 接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMdmDeptSyncManager {

    /**
     * 全量拉取 iHR 部门并逐部门落库（拉取 → 贴源 → 规范化 → 主档 → 事件 → 停用比对 → 核对）
     *
     * <p>单部门处理失败不阻断其余部门（逐条隔离）；处理结果按
     * 成功 / 跳过（无变化）/ 失败分类统计。本方法含 HTTP 调用，不持有数据库事务；
     * 事务边界在 {@code IMdmDeptIngestService.ingestOne} 单部门粒度内。</p>
     *
     * <p>停用比对双守卫：拉取结果为空（防 iHR 异常返回空导致误停用全量）或
     * 本轮存在拉取失败（防个别部门拉取失败被误判为已删除）时跳过比对。</p>
     *
     * @return 本轮同步结果（落库统计 + 停用数 + 一致性核对）
     * @throws com.cacch.integration.common.exception.BizException 上一轮同步仍在执行中（互斥拒绝）或部门清单拉取失败（整轮终止）时抛出
     */
    DeptSyncResult fullSync();

    /**
     * 全量同步结果
     *
     * @param ingest        逐部门落库统计（明细主体为 iHR 部门 uuid）
     * @param disabledCount 停用比对实际停用的部门数（比对被守卫跳过时为 0）
     * @param verify        mdm_dept 与 ods_ihr_dept 一致性核对结果（比对被守卫跳过时为 null）
     */
    record DeptSyncResult(
            BatchProcessResult<String> ingest,
            int disabledCount,
            IMdmDeptIngestService.DeptVerifyResult verify
    ) {
    }
}
