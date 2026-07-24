package com.cacch.integration.integration.oa.client.dto;

import tools.jackson.databind.JsonNode;

/**
 * 致远 OA 附件上传结果
 *
 * @param fileUrl     文件 ID，对应响应 {@code atts[0].fileUrl}，写入表单 field0218
 * @param fileName    上传时的文件名
 * @param rawResponse 致远原始响应 JSON
 * @author hongfu_zhou@cacch.com
 */
public record OaFileUploadResult(
        String fileUrl,
        String fileName,
        JsonNode rawResponse
) {
}
