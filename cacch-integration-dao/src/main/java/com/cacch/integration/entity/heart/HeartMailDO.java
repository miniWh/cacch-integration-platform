package com.cacch.integration.entity.heart;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.integration.dao.typehandler.PostgreSqlJsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Heart 来源邮件数据 DO
 *
 * <p>保存 IMAP 拉取的邮件正文与 PDF 附件元信息，供邮件→PO 解析消费。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName(value = "t_integration_heart_mail", autoResultMap = true)
public class HeartMailDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 去重键：Message-ID，缺失时为 UID:{mail_uid}
     */
    private String mailKey;

    /**
     * IMAP 邮件 UID
     */
    private Long mailUid;

    /**
     * 邮件原始 Message-ID
     */
    private String messageId;

    private String subject;

    /**
     * 发件人地址
     */
    private String fromAddress;

    private LocalDateTime receivedAt;

    /**
     * 纯文本正文（超长截断）
     */
    private String bodyText;

    /**
     * HTML 正文（超长截断）
     */
    private String bodyHtml;

    /**
     * PDF 附件数组：[{name, sha256, path, size}]
     */
    @TableField(typeHandler = PostgreSqlJsonbTypeHandler.class)
    private Object attachFiles;

    /**
     * 流程状态
     */
    private String processStatus;

    private String failReason;

    /**
     * 累计重试次数
     */
    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
