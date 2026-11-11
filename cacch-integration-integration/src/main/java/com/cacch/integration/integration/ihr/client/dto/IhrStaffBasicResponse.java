package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * IHR「获取员工基本信息」接口完整响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrStaffBasicResponse extends IhrBaseResponse {

    /**
     * 员工基本信息（32 字段单对象）
     */
    private IhrStaffBasicInfo data;
}
