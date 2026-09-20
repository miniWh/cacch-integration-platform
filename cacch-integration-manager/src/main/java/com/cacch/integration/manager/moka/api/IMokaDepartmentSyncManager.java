package com.cacch.integration.manager.moka.api;

/**
 * Moka 部门同步编排接口 —— 从本地 IHR 部门快照表读取启用状态部门，转换后落库 Moka 表
 *
 * <p>典型调用链：Controller → 本 Manager → IIhrDepartmentService（读取本地 IHR 快照）
 * + IMokaDepartmentService（本地落库）。</p>
 *
 * <p>说明：本接口不再回源调用 IHR 开放平台，IHR 数据刷新由独立的
 * {@code POST /api/v1/ihr/departments/sync} 接口负责，本接口只读 {@code t_integration_ihr_department}
 * 中 {@code department_status='ENABLE'} 的快照。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaDepartmentSyncManager {

    /**
     * 从本地 IHR 部门快照表同步启用状态部门到 Moka 表
     *
     * <p>读取策略：一次性查询 {@code t_integration_ihr_department} 中
     * {@code department_status='ENABLE'} 且未逻辑删除的记录，按 sequence 升序排序。</p>
     *
     * <p>落库策略：
     * <ul>
     *     <li>主表 {@code t_integration_moka_department} — 按 department_code upsert</li>
     *     <li>子表 {@code t_integration_moka_department_localized} — 同步写入，locale 固定 zh_CN，prop_value = name</li>
     * </ul>
     *
     * @return 同步结果（总读取数 / 主表 upsert 成功数 / 子表 upsert 成功数 / 跳过数）
     */
    MokaDeptSyncResult syncFromIhr();

    /**
     * 同步执行结果
     *
     * @param totalFetched      从本地 IHR 快照表读取的部门总数
     * @param deptUpserted      主表 upsert 成功条数
     * @param localizedUpserted 子表 upsert 成功条数
     * @param deptSkipped       因缺失 departmentCode 等校验失败而跳过的条数
     */
    record MokaDeptSyncResult(int totalFetched, int deptUpserted, int localizedUpserted, int deptSkipped) {
    }
}
