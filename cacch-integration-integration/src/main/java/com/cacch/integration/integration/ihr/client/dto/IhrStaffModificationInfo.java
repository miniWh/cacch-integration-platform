package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;

/**
 * IHR「获取当日更新员工 ID 清单 V2」单条数据
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrStaffModificationInfo {

    /**
     * 员工 ID
     */
    private String staffId;

    /**
     * 员工最后一次信息变更时间
     */
    private Long lastUpdate;
}
