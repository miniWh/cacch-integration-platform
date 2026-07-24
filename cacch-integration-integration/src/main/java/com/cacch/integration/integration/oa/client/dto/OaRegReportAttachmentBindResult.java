package com.cacch.integration.integration.oa.client.dto;

import tools.jackson.databind.JsonNode;

/**
 * 国内登记报告资料附件上传并绑定结果
 *
 * @param fileUrl             REST 上传返回的文件 ID
 * @param fileName            上传文件名
 * @param subReference        写入 field0218 的 subReference
 * @param uploadRawResponse   上传接口原始响应
 * @param batchUpdateResponse batch-update 原始响应
 * @author hongfu_zhou@cacch.com
 */
public record OaRegReportAttachmentBindResult(
        String fileUrl,
        String fileName,
        String subReference,
        JsonNode uploadRawResponse,
        JsonNode batchUpdateResponse
) {
}
