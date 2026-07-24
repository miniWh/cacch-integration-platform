package com.cacch.integration.dto.oa.vo;

import tools.jackson.databind.JsonNode;

/**
 * 致远 OA 附件上传结果 VO
 *
 * @param fileUrl     文件 ID（响应 fileUrl），可写入 formson_5464.field0218
 * @param fileName    上传文件名
 * @param rawResponse 致远原始响应 JSON
 * @author hongfu_zhou@cacch.com
 */
public record OaFileUploadVO(
        String fileUrl,
        String fileName,
        JsonNode rawResponse
) {
}
