package com.cacch.integration.manager.moka.api;

/**
 * Moka 人员同步编排接口 —— 从 persondetail 外部表拉取全量员工，
 * 关联 ihr_department 得到 department_code，落库中间表
 *
 * <p>典型调用链：Controller → syncFromIhr() → PersondetailMapper.selectAll()
 * （读外部表）→ IIhrDepartmentService.listByIhrDeptIds()（批量 IN 查询）
 * → 内存 Map 匹配 department_code → DTO 转 DO →
 * IMokaPersonService.batchUpsert()（DB 写）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaPersonSyncManager {

    /**
     * 从 persondetail 外部表同步全量员工到 Moka 人员中间表
     *
     * <p>执行流程：
     * <ol>
     *     <li>读取 persondetail 全量员工</li>
     *     <li>收集所有 departmentId，批量查询 ihr_department 得到 department_code</li>
     *     <li>逐条字段映射：persondetail → MokaPersonDO</li>
     *     <li>departmentId 匹配不到 department_code 时置 null 并跳过</li>
     *     <li>superiorsInfo 解析提取直属邮箱（结构未知时置 null，不阻塞）</li>
     *     <li>roleId 阶段一不赋值，由 Service 层兜底 DEFAULT 223379</li>
     *     <li>批量 upsert 到 t_integration_moka_person</li>
     * </ol>
     *
     * <p>本接口只读 persondetail（外部表）+ ihr_department（本地表），
     * 无 HTTP 调用、无跨服务事务。</p>
     *
     * @return 同步结果（总读取数 / 有效 upsert 数 / 跳过数）
     */
    MokaPersonSyncResult syncFromIhr();

    /**
     * 同步执行结果
     *
     * @param totalFetched    persondetail 表返回的员工总数
     * @param personUpserted  中间表成功 upsert 的人员数
     * @param deptCodeSkipped 因 departmentId 在 ihr_department 找不到 department_code 被跳过的条数
     * @param invalidSkipped  因 userId 为空等校验失败被跳过的条数
     */
    record MokaPersonSyncResult(int totalFetched, int personUpserted,
                                int deptCodeSkipped, int invalidSkipped) {
    }
}
