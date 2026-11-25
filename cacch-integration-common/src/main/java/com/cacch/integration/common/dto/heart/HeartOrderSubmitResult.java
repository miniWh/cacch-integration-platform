package com.cacch.integration.common.dto.heart;

/**
 * Heart 订单提交流程结果
 *
 * @param status        流程状态，取值见 {@code HeartOrderConstants.SUBMIT_STATUS_*}
 * @param message       结果说明
 * @param cacPoNo       取号成功的 CAC PO 流水号；无待处理数据时为 {@code null}
 * @param crmContractNo CRM 订单编号；仅建单成功时有值
 * @author hongfu_zhou@cacch.com
 */
public record HeartOrderSubmitResult(String status, String message, String cacPoNo, String crmContractNo) {
}
