package com.cacch.integration.manager.ihr.api;

/**
 * IHR 部门全量同步编排接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrDeptSyncManager {

    /**
     * 从 IHR 全量拉取部门清单并 upsert 到本地快照表
     *
     * @return 同步结果摘要（拉取条数 / upsert 条数 / 跳过条数）
     */
    IhrDeptSyncResult syncAll();

    /**
     * 同步结果摘要值对象
     */
    record IhrDeptSyncResult(int totalFetched, int upserted, int skipped) {
    }
}
