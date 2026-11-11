package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * IHR「获取员工 ID 清单」接口完整响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrStaffIdsResponse extends IhrBaseResponse {

    /**
     * 业务数据（分页 + 员工 ID 列表）
     */
    private IhrStaffIdsData data;
}
