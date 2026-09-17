package com.cacch.integration.integration.oa.client.dto;

/**
 * NC 单号关联附件行（OA 附件表 ctp_attachment 查询结果）
 *
 * @param id       附件 ID
 * @param fileUrl  文件标识（CTP_ATTACHMENT.FILE_URL，即附件下载接口的 fileId）
 * @param fileName 文件名（CTP_ATTACHMENT.FILENAME）
 * @author hongfu_zhou@cacch.com
 */
public record OaAttachmentRow(String id, String fileUrl, String fileName) {
}
