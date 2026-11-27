package com.cacch.integration.common.dto.heart;

import java.util.List;

/**
 * Heart 订单批量提交结果
 *
 * @param status       整体状态，取值见 {@code HeartOrderConstants.SUBMIT_STATUS_*}
 * @param message      整体说明
 * @param total        本轮处理条数
 * @param successCount 成功条数
 * @param failedCount  失败条数
 * @param hasMore      是否仍有待处理 PO（达到单轮上限时为 {@code true}）
 * @param items        单条处理明细
 * @author hongfu_zhou@cacch.com
 */
public record HeartOrderBatchSubmitResult(String status, String message, int total,
                                          int successCount, int failedCount, boolean hasMore,
                                          List<HeartOrderItemSubmitResult> items) {
}
