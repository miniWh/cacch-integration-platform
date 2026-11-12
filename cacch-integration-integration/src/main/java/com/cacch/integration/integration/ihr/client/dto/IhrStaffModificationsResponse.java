package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * IHR「获取当日更新员工 ID 清单 V2」接口完整响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrStaffModificationsResponse extends IhrBaseResponse {

    /**
     * 业务数据
     */
    private IhrStaffModificationsData data;
}
