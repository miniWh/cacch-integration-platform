package com.cacch.integration.manager.crm.api;

import com.cacch.integration.common.dto.crm.CrmOrderOaSyncResult;

/**
 * CRM 订单 OA 表单同步编排
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ICrmOrderOaSyncManager {

    /**
     * 扫描 PENDING/RETRY 明细并逐条发起 OA 表单
     *
     * <p>仅处理主表明细拉取成功（detail_fetch_status=SUCCESS）的明细；
     * 人员映射失败或发起失败记 RETRY；retry_count 达上限记 FAILED 并告警。</p>
     *
     * @return 本轮同步统计
     */
    CrmOrderOaSyncResult syncPendingDetails();
}
