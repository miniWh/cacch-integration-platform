package com.cacch.integration.common.dto.heart;

/**
 * Heart 订单单条提交结果
 *
 * @param poId          PO 主键
 * @param poNo          PO 单号
 * @param status        提交状态，取值见 {@code HeartOrderConstants.SUBMIT_STATUS_*}
 * @param message       结果说明
 * @param cacPoNo       取号成功的 CAC PO 流水号；取号前失败时为 {@code null}
 * @param crmContractNo CRM 订单编号；仅建单成功时有值
 * @author hongfu_zhou@cacch.com
 */
public record HeartOrderItemSubmitResult(Long poId, String poNo, String status, String message,
                                         String cacPoNo, String crmContractNo) {
}
