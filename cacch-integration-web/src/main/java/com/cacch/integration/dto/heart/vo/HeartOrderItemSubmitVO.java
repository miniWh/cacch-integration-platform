package com.cacch.integration.dto.heart.vo;

/**
 * Heart 客户 PO 单条提交结果
 *
 * @param poId          PO 主键
 * @param poNo          PO 单号
 * @param status        处理状态：SUCCESS/FAILED
 * @param message       处理说明
 * @param cacPoNo       取号成功的 CAC PO 流水号
 * @param crmContractNo CRM 订单编号，仅建单成功时有值
 * @author hongfu_zhou@cacch.com
 */
public record HeartOrderItemSubmitVO(Long poId, String poNo, String status, String message,
                                     String cacPoNo, String crmContractNo) {
}
