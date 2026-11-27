package com.cacch.integration.manager.heart.api;

import com.cacch.integration.common.dto.heart.HeartOrderBatchSubmitResult;
import com.cacch.integration.common.exception.BizException;

import java.time.LocalDateTime;

/**
 * Heart 客户 PO 订单流程编排 Manager 接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IHeartOrderFlowManager {

    /**
     * 触发一次 Heart 订单流程推进
     *
     * <p>单轮最多处理 {@code HeartOrderConstants.SUBMIT_MAX_PER_ROUND} 条待处理 PO，
     * 单条失败不影响其余记录。开始/结束时间仅作用于邮件拉取入库，为空时取当天。</p>
     *
     * @param startTime 邮件拉取开始时间，可为空
     * @param endTime   邮件拉取结束时间，可为空
     * @return 批量流程结果（整体状态、汇总计数、单条明细）
     * @throws BizException 取号失败（INTEGRATION_ERROR）或回写失败（SYSTEM_ERROR）
     */
    HeartOrderBatchSubmitResult submitOrder(LocalDateTime startTime, LocalDateTime endTime);
}
