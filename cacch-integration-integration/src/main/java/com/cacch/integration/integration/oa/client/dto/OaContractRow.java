package com.cacch.integration.integration.oa.client.dto;

import java.time.LocalDateTime;

/**
 * OA 购销合同扫描行（formmain_5296，已完成 + 有 NC 单号 + 有附件引用 + 来源为 CRM 订单）
 *
 * @param oaFormMainId    购销合同主表 formmain_5296.ID
 * @param oaStartDate     流程发起时间 formmain_5296.START_DATE
 * @param flowStatus      流程状态快照（由 FINISHEDFLAG 按原 SQL CASE 规则转换）
 * @param ncOrderNo       NC 销售订单号 formmain_5296.field0273
 * @param oaContractNo    销售订单编号 formmain_5296.field0104
 * @param oaAttachmentRef 合同附件引用 formmain_5296.field0360
 * @param crmContractId   CRM 合同数据 ID（formmain_2817.field0495，标量子查询，可空）
 * @author hongfu_zhou@cacch.com
 */
public record OaContractRow(Long oaFormMainId,
                            LocalDateTime oaStartDate,
                            String flowStatus,
                            String ncOrderNo,
                            String oaContractNo,
                            String oaAttachmentRef,
                            String crmContractId) {
}
