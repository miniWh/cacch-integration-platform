package com.cacch.integration.service.ihr.api;

import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;

/**
 * IHR 组织架构/部门服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrOrgService {

    /**
     * 条件查询部门（封装 token 获取 + IHR 部门查询 v3）
     *
     * @param request 分页与搜索条件
     * @return IHR 部门分页结果
     */
    IhrOrgSearchResponse searchDepartments(IhrOrgSearchRequest request);
}
