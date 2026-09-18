package com.cacch.integration.dto.oa.vo;

/**
 * NC 单号关联附件下载结果 VO
 *
 * @param fileName    文件名，无匹配时为 null
 * @param contentType 附件 MIME 类型，无匹配时回退 application/octet-stream
 * @param fileSize    附件字节数
 * @param fileContent Base64 编码的文件内容；体积较原始二进制膨胀约 33%
 * @author hongfu_zhou@cacch.com
 */
public record OaNcAttachmentVO(
        String fileName,
        String contentType,
        long fileSize,
        String fileContent
) {
}
