package com.cacch.integration.manager.moka.api;

/**
 * Moka 部门同步编排接口 —— 从上游（IHR）拉取全量部门，转换后落库 Moka 表
 *
 * <p>典型调用链：Controller → 本 Manager → IIhrOrgManager（上游查询）
 * + IMokaDepartmentService（本地落库）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaDepartmentSyncManager {

    /**
     * 从 IHR 全量同步部门到 Moka 表
     *
     * <p>拉取策略：循环翻页（page=0 起始，size=100），直到 IHR 返回 end=true。</p>
     *
     * <p>落库策略：
     * <ul>
     *     <li>主表 {@code t_integration_moka_department} — 按 department_code upsert</li>
     *     <li>子表 {@code t_integration_moka_department_localized} — 同步写入，locale 固定 zh_CN，prop_value = name</li>
     * </ul>
     *
     * @return 同步结果（总拉取数 / 主表 upsert 成功数 / 子表 upsert 成功数）
     */
    MokaDeptSyncResult syncFromIhr();

    /**
     * 同步执行结果
     *
     * @param totalFetched      从 IHR 拉取的部门总数
     * @param deptUpserted      主表 upsert 成功条数
     * @param localizedUpserted 子表 upsert 成功条数
     * @param deptSkipped       因缺失 departmentCode 等校验失败而跳过的条数
     */
    record MokaDeptSyncResult(int totalFetched, int deptUpserted, int localizedUpserted, int deptSkipped) {
    }
}
