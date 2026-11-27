package com.cacch.integration.integration.heart.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件摘要信息
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeartMailInfo {

    /**
     * IMAP 邮件 UID
     */
    private Long uid;

    /**
     * 邮件 Message-ID
     */
    private String messageId;

    /**
     * 主题
     */
    private String subject;

    /**
     * 发件人地址
     */
    private String from;

    /**
     * 收件时间
     */
    private LocalDateTime receivedAt;

    /**
     * 附件文件名（仅名称，不下载内容）
     */
    private List<String> attachmentNames;

    /**
     * 纯文本正文（仅带内容拉取时有值，超长截断）
     */
    private String bodyText;

    /**
     * HTML 正文（仅带内容拉取时有值，超长截断）
     */
    private String bodyHtml;

    /**
     * PDF 附件（已下载至临时文件，入库后删除）
     */
    private List<HeartMailAttachmentInfo> attachments;

    /**
     * 正文/附件拉取失败原因，非空表示该封邮件内容不完整，需重试
     */
    private String fetchError;
}
