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

    /**
     * 增量同步 Pull 阶段 —— 从 iHR 拉取新增 + 更新员工 ID 清单入队 stg_staff_record
     *
     * <p>3 类接口并发调用（T + T-1 双窗口兜底）：</p>
     * <ol>
     *     <li>GET /staffs/additions — 新增接口</li>
     *     <li>GET /v2/staffs/modifications?staffStatus=IN_SERVICE — 在职更新</li>
     *     <li>GET /v2/staffs/modifications?staffStatus=QUIT — 离职更新</li>
     * </ol>
     *
     * <p>每类接口分别拉 T（今天）+ T-1（昨天）两天，共 6 次 HTTP 调用，
     * 通过 CompletableFuture 并发执行。</p>
     *
     * <p>入队幂等：UNIQUE(staff_id, pull_batch_no) DO UPDATE，
     * 已存在 ADD 保留 ADD；lastUpdate 取较大值。</p>
     *
     * @return 本轮入队统计：按 source 分组（ADD=xx, UPDATE=xx）
     * @throws com.cacch.integration.common.exception.BizException 任一 iHR 接口拉取失败（整轮终止）时抛出
     */
    PullIncrementalResult pullIncrementalToQueue();

    /**
     * 增量同步 Process 阶段 —— 从 stg_staff_record 捞取 PENDING 行逐人落库
     *
     * <p>捞取规则：</p>
     * <ul>
     *     <li>FOR UPDATE SKIP LOCKED 防多实例并发</li>
     *     <li>ORDER BY source='ADD' 先于 source='UPDATE'</li>
     *     <li>UPDATE 内部按 last_update 秒级时间戳升序（早变更先落库）</li>
     * </ul>
     *
     * <p>单员工处理完立即 UPDATE status='PROCESSED'，避免进程挂掉时重跑。</p>
     *
     * @param limit 单次处理上限，建议 500
     * @return 本轮处理结果（明细主体为队列行 staffId）
     */
    BatchProcessResult<String> processIncrementalFromQueue(int limit);

    /**
     * Pull 阶段返回结果
     */
    record PullIncrementalResult(
            int addInserted,
            int addConflicted,
            int updateInserted,
            int updateConflicted,
            String pullBatchNo
    ) {
    }
}
