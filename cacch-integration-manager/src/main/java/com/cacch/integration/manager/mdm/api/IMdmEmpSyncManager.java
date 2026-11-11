package com.cacch.integration.manager.mdm.api;

import com.cacch.integration.common.dto.batch.BatchProcessResult;

/**
 * 人员主数据（MDM）iHR 人员同步 Manager 接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMdmEmpSyncManager {

    /**
     * 全量拉取 iHR 员工并逐人落库（拉取 → 贴源 → 规范化 → 主档 → 任职 → 事件）
     *
     * <p>单员工处理失败不阻断其余员工（逐条隔离）；处理结果按
     * 成功 / 跳过（无变化、证件号缺失、响应无数据）/ 失败分类统计。
     * 本方法含 HTTP 调用，不持有数据库事务；事务边界在
     * {@code IMdmEmpIngestService.ingestOne} 单员工粒度内。</p>
     *
     * @return 批量处理结果（明细主体为 iHR staffId）
     * @throws com.cacch.integration.common.exception.BizException 员工 ID 清单拉取失败（整轮终止）时抛出
     */
    BatchProcessResult<String> pullAndIngest();
}
