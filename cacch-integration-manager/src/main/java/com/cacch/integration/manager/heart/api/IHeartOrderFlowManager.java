package com.cacch.integration.manager.heart.api;

import com.cacch.integration.common.dto.heart.HeartOrderSubmitResult;
import com.cacch.integration.common.exception.BizException;

/**
 * Heart 客户 PO 订单流程编排 Manager 接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IHeartOrderFlowManager {

    /**
     * 触发一次 Heart 订单流程推进
     *
     * @return 流程结果（状态、说明、CAC PO 号）
     * @throws BizException 取号失败（INTEGRATION_ERROR）或回写失败（SYSTEM_ERROR）
     */
    HeartOrderSubmitResult submitOrder();
}
