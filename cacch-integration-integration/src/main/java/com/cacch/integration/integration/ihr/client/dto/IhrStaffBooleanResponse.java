package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * iHR 通用 Boolean data 响应（如修改员工基础信息、创建教育经历）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrStaffBooleanResponse extends IhrBaseResponse {

    /**
     * 业务操作结果：{@code code=0} 且本字段为 {@code true} 表示写入成功（如修改基础信息、创建教育经历）
     */
    private Boolean data;
}