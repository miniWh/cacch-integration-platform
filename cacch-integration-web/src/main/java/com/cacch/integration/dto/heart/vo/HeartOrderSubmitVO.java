package com.cacch.integration.dto.heart.vo;

/**
 * Heart 客户 PO 订单提交结果
 *
 * @param status        处理状态：SUCCESS/FAILED/NO_DATA
 * @param message       处理说明
 * @param crmContractNo CRM 订单编号，仅建单成功时有值
 * @author hongfu_zhou@cacch.com
 */
public record HeartOrderSubmitVO(String status, String message, String crmContractNo) {
}
