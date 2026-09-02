package com.cacch.integration.manager.ihr.api;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;

/**
 * IHR 部门查询编排接口（对外唯一入口）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrOrgManager {

    /**
     * 分页查询 IHR 部门清单
     *
     * @param request 部门查询请求（integration 层 DTO，含分页 + 搜索条件）
     * @return IHR 部门分页响应（integration 层 DTO）
     * @throws BizException IHR 凭证缺失 / 接口调用失败 / 业务 code 非 0 时抛出
     */
    IhrOrgSearchResponse searchDepartments(IhrOrgSearchRequest request);
}
