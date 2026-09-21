package com.cacch.integration.dto.moka.vo;

import com.cacch.integration.manager.moka.api.IMokaRoleSyncManager;
import lombok.Getter;

/**
 * Moka 自定义角色同步结果 VO —— 供 Web 层 API 返回
 *
 * <p>对应 Manager 层 {@link IMokaRoleSyncManager.MokaRoleSyncResult}
 * 记录，通过静态工厂 {@link #from} 转换，避免 Manager 层 record 直接暴露到 Controller。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class MokaRoleSyncResultVO {

    /**
     * Moka 开放平台返回的角色总数
     */
    private final int totalFetched;

    /**
     * 本地 DB 成功 upsert 的角色数
     */
    private final int upserted;

    /**
     * 因缺失 roleId 等校验失败而跳过的条数
     */
    private final int skipped;

    public MokaRoleSyncResultVO(int totalFetched, int upserted, int skipped) {
        this.totalFetched = totalFetched;
        this.upserted = upserted;
        this.skipped = skipped;
    }

    /**
     * 将 Manager 层返回的 record 转换为 Web 层 VO
     *
     * @param result Manager 层同步执行结果
     * @return 对应的 VO 实例
     */
    public static MokaRoleSyncResultVO from(IMokaRoleSyncManager.MokaRoleSyncResult result) {
        return new MokaRoleSyncResultVO(result.totalFetched(), result.upserted(), result.skipped());
    }
}
