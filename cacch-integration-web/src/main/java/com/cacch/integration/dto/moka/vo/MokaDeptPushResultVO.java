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
     * Moka 侧新增数量（API 返回）
     */
    private Integer newOnMoka;

    /**
     * Moka 侧更新数量（API 返回）
     */
    private Integer updatedOnMoka;

    /**
     * Moka 侧标记删除数量（API 返回）
     */
    private Integer deletedOnMoka;

    /**
     * 本地 moka_sync_status 成功更新为 1 的条数
     */
    private Integer syncStatusSuccess;

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
        vo.setNewOnMoka(result.newOnMoka());
        vo.setUpdatedOnMoka(result.updatedOnMoka());
        vo.setDeletedOnMoka(result.deletedOnMoka());
        vo.setSyncStatusSuccess(result.syncStatusSuccess());
        return vo;
    }
}
