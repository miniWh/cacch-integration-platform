package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;

import java.util.List;

/**
 * IHR「获取当日更新员工 ID 清单 V2」接口业务数据
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrStaffModificationsData {

    /**
     * 分页信息
     */
    private IhrPageInfo pageInfo;

    /**
     * 员工变更列表
     */
    private List<IhrStaffModificationInfo> dataList;
}
