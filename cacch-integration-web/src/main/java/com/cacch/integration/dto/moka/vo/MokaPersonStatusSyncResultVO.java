package com.cacch.integration.dto.moka.vo;

import com.cacch.integration.manager.moka.api.IMokaPersonStatusSyncManager;
import lombok.Getter;

/**
 * Moka 人员入离职状态同步结果 VO —— 供 Web 层 API 返回
 *
 * <p>对应 Manager 层 {@link IMokaPersonStatusSyncManager.MokaPersonStatusSyncResult}
 * record，通过静态工厂 {@link #from} 转换，避免 Manager 层 record
 * 直接暴露到 Controller。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class MokaPersonStatusSyncResultVO {

    /**
     * 读取的待处理记录总数（入职 + 离职）
     */
    private final int totalRead;

    /**
     * 入职同步成功条数（Moka 成功且状态行回写成功）
     */
    private final int hiredSuccess;

    /**
     * 入职同步失败条数（Moka 调用失败，含业务失败与异常）
     */
    private final int hiredFailed;

    /**
     * 离职同步成功条数（Moka 成功且状态行回写成功）
     */
    private final int resignSuccess;

    /**
     * 离职同步失败条数（Moka 调用失败，含业务失败与异常）
     */
    private final int resignFailed;

    /**
     * applicationId 为空被跳过的条数（不写状态行）
     */
    private final int skippedNoApplication;

    /**
     * 状态行回写失败条数（含 Moka 成功后回写失败）
     */
    private final int dbUpdateFailed;

    public MokaPersonStatusSyncResultVO(int totalRead, int hiredSuccess, int hiredFailed,
                                        int resignSuccess, int resignFailed,
                                        int skippedNoApplication, int dbUpdateFailed) {
        this.totalRead = totalRead;
        this.hiredSuccess = hiredSuccess;
        this.hiredFailed = hiredFailed;
        this.resignSuccess = resignSuccess;
        this.resignFailed = resignFailed;
        this.skippedNoApplication = skippedNoApplication;
        this.dbUpdateFailed = dbUpdateFailed;
    }

    /**
     * 将 Manager 层返回的 record 转换为 Web 层 VO
     *
     * @param result Manager 层同步执行结果
     * @return 对应的 VO 实例
     */
    public static MokaPersonStatusSyncResultVO from(IMokaPersonStatusSyncManager.MokaPersonStatusSyncResult result) {
        return new MokaPersonStatusSyncResultVO(result.totalRead(), result.hiredSuccess(),
                result.hiredFailed(), result.resignSuccess(), result.resignFailed(),
                result.skippedNoApplication(), result.dbUpdateFailed());
    }
}
