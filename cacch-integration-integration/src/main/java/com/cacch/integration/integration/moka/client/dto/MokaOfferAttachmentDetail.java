package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka offer 扩展信息 —— offer 附件元素（含入职日期等关键字段）
 *
 * <p>对应 Moka 响应中 {@code data.offerAttachmentList[]} 的元素结构。
 * <strong>重要</strong>：Moka 实际返回的 {@code checkInDate} 不在
 * {@link MokaOfferExtendInfoResponse.OfferExtendInfo} 顶层，而是在本类的
 * {@link #checkInDate} 字段中——这与最初建模假设不同，已通过 raw 日志确认。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaOfferAttachmentDetail {

    /**
     * 附件记录 ID
     */
    private Long id;

    /**
     * offer ID
     */
    private Long offerId;

    /**
     * 入职日期（格式 yyyy-MM-dd）
     *
     * <p>Moka 实际返回的入职日期字段位于此，而非 OfferExtendInfo 顶层的 checkInDate。</p>
     */
    private String checkInDate;

    /**
     * 入职日期字符串（冗余，与 checkInDate 值相同）
     */
    private String checkInDateStr;

    /**
     * 试用期结束日期（格式 yyyy-MM-dd，可能为 null）
     */
    private String probationEndDate;

    /**
     * 试用期结束日期字符串（冗余，与 probationEndDate 值相同）
     */
    private String probationEndDateStr;
}
