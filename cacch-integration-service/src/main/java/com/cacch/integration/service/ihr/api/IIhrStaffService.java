package com.cacch.integration.service.ihr.api;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicInfo;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicResponse;

import java.util.List;

/**
 * IHR 员工域业务 Service 接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrStaffService {

    /**
     * 全量拉取员工 ID 清单（内部循环翻页直到末页）
     *
     * <p>调用方无需关心分页实现，本方法一次返回合并后的完整 ID 列表。</p>
     *
     * @param staffStatus 员工状态过滤：
     *                    <ul>
     *                        <li>{@link com.cacch.integration.common.constant.ihr.IhrStaffConstants#STAFF_STATUS_IN_SERVICE} — 仅在职</li>
     *                        <li>{@link com.cacch.integration.common.constant.ihr.IhrStaffConstants#STAFF_STATUS_QUIT} — 仅离职</li>
     *                        <li>null 或空字符串 — 全部状态</li>
     *                    </ul>
     * @return 合并后的员工 ID 字符串列表；无数据时返回空列表，不会返回 null
     * @throws BizException iHR 接口返回业务失败码、HTTP 异常或重试后仍失败时抛出
     */
    List<String> fetchAllStaffIds(String staffStatus);

    /**
     * 按 staffId 查询单个员工的基本信息（完整响应，含原始报文原文）
     *
     * <p>供人员主数据治理链路使用：除业务数据外，
     * {@link IhrStaffBasicResponse#getRawJson()} 携带 iHR 原始响应原文，
     * 用于 stg 层贴源落库（stg_ihr_emp.raw_json）。</p>
     *
     * @param staffId 员工 ID；不得为 null 或空字符串
     * @return iHR 完整响应（非 null，含 rawJson 原文；data 可能为 null）
     * @throws BizException staffId 为空、iHR 接口返回业务失败码、重试后仍失败时抛出
     */
    IhrStaffBasicResponse fetchStaffBasicResponse(String staffId);

    /**
     * 按 staffId 查询单个员工的基本信息
     *
     * @param staffId 员工 ID；不得为 null 或空字符串
     * @return 员工基本资料；iHR 响应无 data 时返回 null
     * @throws BizException                                        staffId 为空、iHR 接口返回业务失败码、重试后仍失败时抛出
     * @throws com.cacch.integration.common.exception.BizException staffId 为空时抛出
     */
    IhrStaffBasicInfo fetchStaffBasic(String staffId);
}
