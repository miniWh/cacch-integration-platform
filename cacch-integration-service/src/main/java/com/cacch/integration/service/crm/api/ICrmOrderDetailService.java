package com.cacch.integration.service.crm.api;

import com.cacch.integration.entity.crm.CrmOrderDetailDO;

/**
 * CRM 订单明细持久化服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmOrderDetailService {

    /**
     * 按 CRM 明细 ID 判断是否已存在
     *
     * @param crmDetailId CRM 明细 id
     * @return true 表示已存在
     */
    boolean existsByCrmDetailId(String crmDetailId);

    /**
     * 新增明细（仅新增）
     *
     * @param detail 明细实体
     * @return 插入后实体
     */
    CrmOrderDetailDO insert(CrmOrderDetailDO detail);
}
