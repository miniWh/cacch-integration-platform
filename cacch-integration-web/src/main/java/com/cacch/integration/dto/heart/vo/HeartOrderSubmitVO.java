package com.cacch.integration.dto.heart.vo;

/**
 * Heart 客户 PO 订单提交结果
 *
 * @param status  受理状态，当前空实现固定返回 PENDING
 * @param message 受理说明
 * @author hongfu_zhou@cacch.com
 */
public record HeartOrderSubmitVO(String status, String message) {
}
