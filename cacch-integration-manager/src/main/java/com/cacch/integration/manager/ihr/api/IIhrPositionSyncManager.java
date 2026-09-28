package com.cacch.integration.manager.ihr.api;

/**
 * IHR 职位全量同步编排接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrPositionSyncManager {

    /**
     * 从 IHR 全量拉取职位清单并 upsert 到本地快照表
     *
     * @return 同步结果摘要（拉取条数 / upsert 条数 / 跳过条数）
     */
    PositionSyncResult syncAll();

    /**
     * 同步结果摘要值对象
     */
    record PositionSyncResult(int totalFetched, int upserted, int skipped) {
    }
}
