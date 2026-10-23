package com.cacch.integration.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OA 货物购销合同附件回传 CRM 中间记录表实体，映射 t_integration_oa_contract_attachment_sync
 *
 * <p>同时保存 OA 侧合同数据快照与回传 CRM 的同步状态，充当扫描幂等键与状态查询载体。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("t_integration_oa_contract_attachment_sync")
public class OaContractAttachmentSyncDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * OA 购销合同主表 formmain_5296.ID，幂等唯一键
     */
    private Long oaFormMainId;

    /**
     * OA 流程发起时间 formmain_5296.START_DATE
     */
    private LocalDateTime oaStartDate;

    /**
     * 流程状态快照：已完成 / 未完成 / 未知
     */
    private String flowStatus;

    /**
     * NC 销售订单号 formmain_5296.FIELD0273
     */
    private String ncOrderNo;

    /**
     * 销售订单编号 formmain_5296.FIELD0104
     */
    private String oaContractNo;

    /**
     * 合同附件引用 formmain_5296.FIELD0360，对应 ctp_attachment.sub_reference
     */
    private String oaAttachmentRef;

    /**
     * OA 附件主键 ctp_attachment.ID
     */
    private String oaAttachmentId;

    /**
     * OA 附件文件标识 ctp_attachment.FILE_URL（fileId，非公网地址）
     */
    private String attachmentFileId;

    /**
     * OA 附件文件名 ctp_attachment.FILENAME
     */
    private String attachmentFileName;

    /**
     * 回传 CRM 的附件访问地址
     */
    private String attachmentUrl;

    /**
     * CRM 合同数据 ID（contractEdit 的 data_id）
     */
    private String crmContractId;

    /**
     * 同步状态，取值见 OaContractAttachmentSyncStatusEnum
     */
    private String syncStatus;

    /**
     * 业务跳过原因，如 OA附件缺失
     */
    private String skipReason;

    /**
     * 同步说明或失败原因（含 CRM return_code / return_msg）
     */
    private String syncMessage;

    /**
     * 累计失败次数，仅用于观测，不设上限
     */
    private Integer retryCount;

    /**
     * 乐观锁版本号，抢占记录时 CAS 比对
     */
    private Integer version;

    /**
     * 最近一次同步时间，PROCESSING 超时回收依据
     */
    private LocalDateTime lastSyncAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-正常 1-删除
     */
    @TableLogic
    private Integer isDeleted;
}
