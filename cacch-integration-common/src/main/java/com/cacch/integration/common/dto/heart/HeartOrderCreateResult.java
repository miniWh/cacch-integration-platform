package com.cacch.integration.common.dto.heart;

/**
 * Heart PO 建单结果
 *
 * @param success       是否建单成功（含幂等跳过）
 * @param skipped       是否因已建单而跳过
 * @param message       结果说明
 * @param crmContractId CRM 订单数据 ID
 * @param crmContractNo CRM 订单编号
 * @author hongfu_zhou@cacch.com
 */
public record HeartOrderCreateResult(boolean success, boolean skipped, String message,
                                     String crmContractId, String crmContractNo) {

    /**
     * 建单成功
     *
     * @param crmContractId CRM 订单数据 ID
     * @param crmContractNo CRM 订单编号
     * @return 成功结果
     */
    public static HeartOrderCreateResult created(String crmContractId, String crmContractNo) {
        return new HeartOrderCreateResult(true, false,
                "CRM 建单成功, crmContractNo=" + crmContractNo, crmContractId, crmContractNo);
    }

    /**
     * 已建单，幂等跳过
     *
     * @param crmContractId CRM 订单数据 ID
     * @param crmContractNo CRM 订单编号
     * @return 跳过结果
     */
    public static HeartOrderCreateResult skipped(String crmContractId, String crmContractNo) {
        return new HeartOrderCreateResult(true, true,
                "PO 已建单，跳过, crmContractNo=" + crmContractNo, crmContractId, crmContractNo);
    }

    /**
     * 建单失败
     *
     * @param message 失败原因
     * @return 失败结果
     */
    public static HeartOrderCreateResult failed(String message) {
        return new HeartOrderCreateResult(false, false, message, null, null);
    }
}
