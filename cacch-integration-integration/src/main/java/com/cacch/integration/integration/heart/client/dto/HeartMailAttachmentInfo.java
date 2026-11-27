package com.cacch.integration.integration.heart.client.dto;

import lombok.Data;

/**
 * 邮件 PDF 附件信息
 *
 * <p>附件内容以下载后的临时文件承载，入库前由 Service 移动至文件服务器并删除临时文件。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class HeartMailAttachmentInfo {

    /**
     * 附件文件名（MIME 解码后）
     */
    private String name;

    /**
     * 文件 SHA-256（边下载边计算）
     */
    private String sha256;

    /**
     * 文件大小（字节）
     */
    private long size;

    /**
     * 临时文件绝对路径
     */
    private String tempFilePath;
}
