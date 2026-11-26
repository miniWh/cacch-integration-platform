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
}
