package com.cacch.integration.dto.ihr.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * IHR 部门树员工查询请求
 *
 * <p>仅含 {@code lastUpdateDate} 一个可选参数：
 * <ul>
 *     <li>不传或为 null — 返回指定部门树下全部在职员工</li>
 *     <li>传入日期 — 仅返回 {@code ihr_staff_update_sync_record.last_update >= 该日期} 的员工</li>
 * </ul>
 *
 * <p>注意：{@code ihrDeptId} 由 Controller 写死，不在请求体中暴露。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class DeptStaffRequest {

    /**
     * 最后更新日期（可选，格式 yyyy-MM-dd）
     *
     * <p>传入时只返回 {@code ihr_staff_update_sync_record.last_update >= 该日期} 的员工；
     * 不传时返回部门树下全部在职员工。</p>
     */
    private LocalDate lastUpdateDate;
}
