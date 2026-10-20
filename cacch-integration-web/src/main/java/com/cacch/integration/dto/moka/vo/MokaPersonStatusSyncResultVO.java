package com.cacch.integration.dto.moka.vo;

import com.cacch.integration.manager.moka.api.IMokaPersonStatusSyncManager;

/**
 * Moka 候选人入离职状态同步结果 VO —— 供 Web 层 API 返回
 *
 * <p>对应 Manager 层 {@link IMokaPersonStatusSyncManager.MokaPersonStatusSyncResult}
 * record，通过静态工厂 {@link #from} 转换，避免 Manager 层 record
 * 直接暴露到 Controller。</p>
 *
 * @param totalRead      读取的待处理记录总数（入职 + 离职，已 INNER JOIN candidate_base 过滤无候选人）
 * @param hiredSuccess   入职同步成功条数（Moka 成功且状态行回写成功）
 * @param hiredFailed    入职同步失败条数（Moka 调用失败，含业务失败与异常）
 * @param resignSuccess  离职同步成功条数（Moka 成功且状态行回写成功）
 * @param resignFailed   离职同步失败条数（Moka 调用失败，含业务失败与异常）
 * @param dbUpdateFailed 状态行回写失败条数（含 Moka 成功后回写失败）
 * @author hongfu_zhou@cacch.com
 */
public record MokaPersonStatusSyncResultVO(int totalRead, int hiredSuccess, int hiredFailed, int resignSuccess,
                                           int resignFailed, int dbUpdateFailed) {

    /**
     * 将 Manager 层返回的 record 转换为 Web 层 VO
     *
     * @param result Manager 层同步执行结果
     * @return 对应的 VO 实例
     */
    public static MokaPersonStatusSyncResultVO from(IMokaPersonStatusSyncManager.MokaPersonStatusSyncResult result) {
        return new MokaPersonStatusSyncResultVO(result.totalRead(), result.hiredSuccess(),
                result.hiredFailed(), result.resignSuccess(), result.resignFailed(),
                result.dbUpdateFailed());
    }
}
