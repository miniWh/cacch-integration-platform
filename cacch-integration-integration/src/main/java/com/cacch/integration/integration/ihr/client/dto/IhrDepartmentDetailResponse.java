package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * IHR 获取部门详情 — 响应体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrDepartmentDetailResponse extends IhrBaseResponse {

    /**
     * 部门详情数据
     */
    private IhrDepartment data;

    /**
     * iHR 原始响应报文
     */
    private String rawJson;
}
