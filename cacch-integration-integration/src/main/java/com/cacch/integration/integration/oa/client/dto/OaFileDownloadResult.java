package com.cacch.integration.integration.oa.client.dto;

/**
 * 致远 OA 附件文件下载结果
 *
 * @param fileName    文件名，取自响应 Content-Disposition，可能为 null
 * @param contentType 响应 MIME 类型，可能为 null
 * @param content     文件内容，不为 null
 * @param fileUrl     OA 附件表 FILE_URL（即下载接口的 fileId），由调用方传入
 * @author hongfu_zhou@cacch.com
 */
public record OaFileDownloadResult(String fileName, String contentType, byte[] content, String fileUrl) {

    /**
     * 获取文件内容长度
     *
     * @return 内容字节数
     */
    public long contentLength() {
        return content == null ? 0L : content.length;
    }
}
