package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;

import java.util.List;

/**
 * IHR「获取员工 ID 清单」接口业务数据
 *
 * <p>响应体结构：code / message / errorResult（继承自 IhrBaseResponse）+ data.pageInfo / data.dataList（本类）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrStaffIdsData {

    /**
     * 分页信息
     */
    private IhrPageInfo pageInfo;

    /**
     * 员工 ID 字符串列表
     */
    private List<String> dataList;
}
