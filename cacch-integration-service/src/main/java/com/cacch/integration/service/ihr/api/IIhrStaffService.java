package com.cacch.integration.service.ihr.api;

import com.cacch.integration.common.exception.BizException;

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
}
