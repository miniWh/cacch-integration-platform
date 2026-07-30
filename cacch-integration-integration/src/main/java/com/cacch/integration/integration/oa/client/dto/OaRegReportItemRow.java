package com.cacch.integration.integration.oa.client.dto;

/**
 * OA 国内登记报告资料列表行（主表 + 子表 JOIN 结果）
 *
 * @author hongfu_zhou@cacch.com
 */
public record OaRegReportItemRow(
        String formMainId,
        String ownerName,
        String ipdpName,
        String ipdpProjectNo,
        String subRowId,
        String itemName,
        String currentAttachmentRef
) {
}
