package com.cacch.integration.dto.moka.vo;

import com.cacch.integration.manager.moka.api.IMokaDepartmentPushManager;
import lombok.Data;

/**
 * Moka 部门推送到 Moka 开放平台执行结果 VO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptPushResultVO {

    /**
     * 本地 PG 查询出的部门总数（待推送）
     */
    private Integer totalPushed;

    /**
     * 实际推送的条数（等于 totalPushed，全量推送语义）
     */
    private Integer syncedToMoka;

    /**
     * Moka API 调用是否成功
     * true = Moka 返回 code=0（推送成功）
     * false = Moka API 异常或 code≠0（推送失败）
     */
    private Boolean mokaApiSuccess;

    /**
     * Moka 侧新增数量（API 返回；mokaApiSuccess=false 时为 null）
     */
    private Integer newOnMoka;

    /**
     * Moka 侧更新数量（API 返回；mokaApiSuccess=false 时为 null）
     */
    private Integer updatedOnMoka;

    /**
     * Moka 侧标记删除数量（API 返回；mokaApiSuccess=false 时为 null）
     */
    private Integer deletedOnMoka;

    /**
     * 本地 moka_sync_status 更新为 1（SYNCED）的条数
     * Moka API 成功时才会有值，失败时为 0
     */
    private Integer syncedCount;

    /**
     * 本地 moka_sync_status 更新为 2（SYNC_FAILED）的条数
     * Moka API 失败时才会有值，成功时为 0
     */
    private Integer syncFailedCount;

    /**
     * DB 状态更新自身抛异常的条数（逐条 try-catch 累计）
     * 应始终为 0；非 0 说明 DB 有问题需要排查
     */
    private Integer dbUpdateFailedCount;

    /**
     * 构造 VO
     *
     * @param result Manager 层推送执行结果
     * @return VO 实例
     */
    public static MokaDeptPushResultVO from(IMokaDepartmentPushManager.MokaDeptPushResult result) {
        MokaDeptPushResultVO vo = new MokaDeptPushResultVO();
        vo.setTotalPushed(result.totalPushed());
        vo.setSyncedToMoka(result.syncedToMoka());
        vo.setMokaApiSuccess(result.mokaApiSuccess());
        vo.setNewOnMoka(result.newOnMoka());
        vo.setUpdatedOnMoka(result.updatedOnMoka());
        vo.setDeletedOnMoka(result.deletedOnMoka());
        vo.setSyncedCount(result.syncedCount());
        vo.setSyncFailedCount(result.syncFailedCount());
        vo.setDbUpdateFailedCount(result.dbUpdateFailedCount());
        return vo;
    }
}
