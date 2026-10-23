package com.cacch.integration.dto.oa.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * OA 货物购销合同附件回传 CRM 同步状态 VO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class OaContractAttachmentSyncRecordVO {

    /**
     * 中间记录主键
     */
    private Long id;

    /**
     * OA 购销合同主表 formmain_5296.ID
     */
    private Long oaFormMainId;

    /**
     * OA 流程发起时间
     */
    private LocalDateTime oaStartDate;

    /**
     * 流程状态：已完成 / 未完成 / 未知
     */
    private String flowStatus;

    /**
     * NC 销售订单号
     */
    private String ncOrderNo;

    /**
     * 销售订单编号
     */
    private String oaContractNo;

    /**
     * 合同附件引用（field0360）
     */
    private String oaAttachmentRef;

    /**
     * OA 附件主键
     */
    private String oaAttachmentId;

    /**
     * OA 附件文件标识
     */
    private String attachmentFileId;

    /**
     * OA 附件文件名
     */
    private String attachmentFileName;

    /**
     * 回传 CRM 的附件访问地址
     */
    private String attachmentUrl;

    /**
     * CRM 合同数据 ID
     */
    private String crmContractId;

    /**
     * 同步状态，取值见 OaContractAttachmentSyncStatusEnum
     */
    private String syncStatus;

    /**
     * 业务跳过原因
     */
    private String skipReason;

    /**
     * 同步说明或失败原因
     */
    private String syncMessage;

    /**
     * 累计失败次数
     */
    private Integer retryCount;

    /**
     * 最近一次同步时间
     */
    private LocalDateTime lastSyncAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
