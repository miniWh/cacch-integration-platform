package com.cacch.integration.dto.heart.vo;

import java.util.List;

/**
 * Heart 客户 PO 订单批量提交结果
 *
 * @param status       整体处理状态：SUCCESS/PARTIAL/FAILED/NO_DATA
 * @param message      整体处理说明
 * @param total        本轮处理条数
 * @param successCount 成功条数
 * @param failedCount  失败条数
 * @param hasMore      是否仍有待处理 PO
 * @param items        单条处理明细
 * @author hongfu_zhou@cacch.com
 */
public record HeartOrderBatchSubmitVO(String status, String message, int total,
                                      int successCount, int failedCount, boolean hasMore,
                                      List<HeartOrderItemSubmitVO> items) {
}
