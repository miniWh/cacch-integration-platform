package com.cacch.integration.dto.moka.vo;

import com.cacch.integration.manager.moka.api.IMokaPersonPushManager;
import lombok.Getter;

/**
 * Moka 人员推送结果 VO —— 供 Web 层 API 返回
 *
 * <p>对应 Manager 层 {@link IMokaPersonPushManager.MokaPersonPushResult}
 * record，通过静态工厂 {@link #from} 转换，避免 Manager 层 record
 * 直接暴露到 Controller。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class MokaPersonPushResultVO {

    /**
     * 从中间表读取的待推送记录总数
     */
    private final int totalRead;

    /**
     * 实际推送的批次数（每批 ≤ 100 条）
     */
    private final int batchCount;

    /**
     * Moka API 整体调用是否成功（所有批次都成功才为 true）
     */
    private final boolean mokaApiSuccess;

    /**
     * 推送成功并更新为 SYNCED 的条数
     */
    private final int syncedCount;

    /**
     * 推送失败并更新为 SYNC_FAILED 的条数
     * （含 Moka API 业务失败 + HTTP 调用异常）
     */
    private final int syncFailedCount;

    /**
     * DB 状态回写失败的条数
     */
    private final int dbUpdateFailedCount;

    public MokaPersonPushResultVO(int totalRead, int batchCount, boolean mokaApiSuccess,
                                  int syncedCount, int syncFailedCount,
                                  int dbUpdateFailedCount) {
        this.totalRead = totalRead;
        this.batchCount = batchCount;
        this.mokaApiSuccess = mokaApiSuccess;
        this.syncedCount = syncedCount;
        this.syncFailedCount = syncFailedCount;
        this.dbUpdateFailedCount = dbUpdateFailedCount;
    }

    /**
     * 将 Manager 层返回的 record 转换为 Web 层 VO
     *
     * @param result Manager 层推送执行结果
     * @return 对应的 VO 实例
     */
    public static MokaPersonPushResultVO from(IMokaPersonPushManager.MokaPersonPushResult result) {
        return new MokaPersonPushResultVO(result.totalRead(), result.batchCount(),
                result.mokaApiSuccess(), result.syncedCount(),
                result.syncFailedCount(), result.dbUpdateFailedCount());
    }
}
