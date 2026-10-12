package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka offer 扩展信息 —— 附件元素（候选人附件 / 审批人附件共用）
 *
 * <p>对应 Moka 响应中 {@code data.candidateAttachmentList[]} 与
 * {@code data.approverAttachmentList[]} 的元素结构。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaOfferAttachment {

    /**
     * 附件名称
     */
    private String name;

    /**
     * 附件下载链接（URL，通常带有效期）
     */
    private String downloadUrl;
}
