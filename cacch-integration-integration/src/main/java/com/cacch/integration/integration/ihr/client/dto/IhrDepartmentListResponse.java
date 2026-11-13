package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * IHR 根据部门 id 集合获取部门详情 — 响应体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrDepartmentListResponse extends IhrBaseResponse {

    /**
     * 部门详情列表
     */
    private List<IhrDepartment> data;

    /**
     * iHR 原始响应报文
     */
    private String rawJson;
}
