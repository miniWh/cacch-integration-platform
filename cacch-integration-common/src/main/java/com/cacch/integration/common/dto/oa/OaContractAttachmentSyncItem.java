package com.cacch.integration.common.dto.oa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OA 货物购销合同附件回传 CRM 处理明细主体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OaContractAttachmentSyncItem {

    /**
     * OA 购销合同主表 ID（formmain_5296.ID）
     */
    private Long oaFormMainId;

    /**
     * NC 销售订单号（formmain_5296.FIELD0273）
     */
    private String ncOrderNo;

    /**
     * CRM 合同数据 ID（contractEdit 的 data_id）
     */
    private String crmContractId;
}
