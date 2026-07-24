package com.cacch.integration.dto.oa.vo;

import tools.jackson.databind.JsonNode;

/**
 * 国内登记报告资料附件上传并绑定结果 VO
 *
 * @param fileUrl             REST 上传返回的文件 ID
 * @param fileName            上传文件名；仅绑定时可能为 null
 * @param subReference        写入 field0218 的 subReference（非 fileUrl）
 * @param uploadRawResponse   上传接口原始响应；仅绑定时为 null
 * @param batchUpdateResponse CAP4 batch-update 原始响应
 * @author hongfu_zhou@cacch.com
 */
public record OaRegReportAttachmentBindVO(
        String fileUrl,
        String fileName,
        String subReference,
        JsonNode uploadRawResponse,
        JsonNode batchUpdateResponse
) {
}
