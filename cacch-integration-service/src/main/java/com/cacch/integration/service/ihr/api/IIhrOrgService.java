package com.cacch.integration.service.ihr.api;

import com.cacch.integration.integration.ihr.client.dto.IhrDepartmentDetailResponse;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;

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
     * 查询单个部门详情
     *
     * @param deptId 部门 ID（部门清单 v3 返回的 {@code id} 字段），不可为空
     * @return 部门详情响应（含 rawJson 原文，供贴源留底）
     * @throws com.cacch.integration.common.exception.BizException deptId 为空、HTTP 调用失败（含重试后）或业务 code 非 0 时抛出
     */
    IhrDepartmentDetailResponse getDepartmentDetail(String deptId);
}
