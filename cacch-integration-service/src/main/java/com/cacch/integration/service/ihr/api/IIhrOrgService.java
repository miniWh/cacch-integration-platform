package com.cacch.integration.service.ihr.api;

import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentDetailResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;

import java.util.List;

/**
 * IHR 组织架构/部门服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrOrgService {

    /**
     * 条件查询部门
     *
     * @param request 分页与搜索条件
     * @return IHR 部门分页结果
     */
    IhrOrgSearchResponse searchDepartments(IhrOrgSearchRequest request);

    /**
     * 全量拉取 iHR 部门清单（获取部门清单 v3，分页翻页至末页）
     *
     * <p>按 {@code page} 从 0 起、每页 100 条、无搜索条件（全量）循环拉取，
     * 以 {@code end=true} 或 {@code page 达到 totalPages-1} 终止；
     * 每页校验业务 code（非 0 抛 BizException），鉴权失败自动强刷 token 重试
     * （复用 {@link #searchDepartments} 内部重试逻辑）。首页即空时返回空列表。</p>
     *
     * @return iHR 全量部门列表（含 uuid / departmentCode / parentDepartmentCode 等）；无数据时返回空列表（非 null）
     * @throws com.cacch.integration.common.exception.BizException 任一页 HTTP 调用失败（含重试后）或业务 code 非 0 时抛出（整轮终止）
     */
    List<IhrDepartment> fetchAllDepartments();

    /**
     * 查询单个部门详情（按部门编码）
     *
     * @param departmentCode 部门编码
     * @return 部门详情响应（含 rawJson 原文，供贴源留底）
     * @throws com.cacch.integration.common.exception.BizException departmentCode 为空、HTTP 调用失败（含重试后）或业务 code 非 0 时抛出
     */
    IhrDepartmentDetailResponse getDepartmentDetail(String departmentCode);
}
