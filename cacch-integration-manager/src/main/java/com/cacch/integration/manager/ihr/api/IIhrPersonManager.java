package com.cacch.integration.manager.ihr.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * IHR 人员查询编排接口（对外唯一入口）
 *
 * <p>典型调用链：Controller → 本 Manager → IIhrPersonService → PersondetailMapper。
 * 仅做查询编排，无跨聚合写操作、无 HTTP 调用、无事务控制。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrPersonManager {

    /**
     * 按 IHR 部门树递归查询在职员工
     *
     * <p>查询逻辑：
     * <ol>
     *     <li>以 {@code ihrDeptId} 为根，递归向下查所有 ENABLE 状态子部门</li>
     *     <li>JOIN persondetail 取部门树下所有在职员工</li>
     *     <li>LEFT JOIN ihr_staff_update_sync_record 附加 {@code last_update} 字段</li>
     *     <li>{@code lastUpdateDate} 非空时追加时间过滤：{@code last_update >= lastUpdateDate}</li>
     * </ol>
     *
     * @param ihrDeptId      IHR 部门 ID（{@code t_integration_ihr_department.ihr_dept_id}），不可为空
     * @param lastUpdateDate 最后更新日期下界（可选）；为 null 时返回全部
     * @return 员工记录列表（每条为 Map，含 persondetail 所有字段 + 部门信息 + last_update）；无数据时返回空列表
     */
    List<Map<String, Object>> listStaffByDeptTree(String ihrDeptId, LocalDate lastUpdateDate);
}
