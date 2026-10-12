package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka offer 扩展信息 —— 发送历史记录元素
 *
 * <p>对应 Moka 响应中 {@code data.offerHistoryList[]} 的元素结构，含发送记录
 * 元信息（id/状态/发送渠道）与嵌套的 offerDetermins 拒绝决定详情。</p>
 *
 * <p>注意：{@code offerDetermins} 字段名按 Moka 文档原样保留（疑似 Moka 侧
 * 拼写为 "Determins"，非 "Determines"）；反序列化时使用 {@link com.fasterxml.jackson.annotation.JsonProperty}
 * 显式指定以避免 Jackson 自动命名转换。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaOfferHistory {

    /**
     * 发送记录 ID
     */
    private Long id;

    /**
     * 公司 ID（租户 ID）
     */
    private String orgId;

    /**
     * 申请 ID
     */
    private Long applicationId;

    /**
     * offer ID
     */
    private Long offerId;

    /**
     * Offer 发送时间（时间戳毫秒）
     */
    private Long sentAt;

    /**
     * offer 状态（pending/accepted/rejected/invalid/expired）
     */
    private String status;

    /**
     * 是否发送 SMS
     */
    private Boolean notifySms;

    /**
     * 是否发送 Email
     */
    private Boolean notifyEmail;

    /**
     * 拒绝原因标识：0=不存在，1=存在，-1=无权限
     */
    private Integer existReason;

    /**
     * 拒绝决定详情
     */
    private OfferDetermins offerDetermins;

    /**
     * 发送人 ID
     */
    private Long sentUserId;

    /**
     * offer 拒绝决定详情 —— 嵌套在 offerHistoryList 元素中
     *
     * <p>字段名 {@code offerDetermins} 按 Moka 文档原样保留，反序列化时显式指定。</p>
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class OfferDetermins {

        /**
         * 业务 ID
         */
        private Long id;

        /**
         * 租户 ID
         */
        private String orgId;

        /**
         * 申请 ID
         */
        private Long applicationId;

        /**
         * 发送记录 ID
         */
        private Long historyId;

        /**
         * 记录状态：accepted / rejected
         */
        private String status;

        /**
         * 人才库 ID
         */
        private Long talentPoolId;

        /**
         * 拒绝原因 ID
         */
        private Long reasonId;

        /**
         * 拒绝原因
         */
        private String reason;

        /**
         * 详细原因
         */
        private String detail;

        /**
         * offer 阶段 ID
         */
        private Long stageId;

        /**
         * 操作类型：1=候选人，2=HR 手动标记，3=API 标记
         */
        private Integer operationType;

        /**
         * 操作人 ID
         */
        private Long operatorId;

        /**
         * 是否有效
         */
        private Boolean validated;

        /**
         * 创建时间（时间戳毫秒）
         */
        private Long createdAt;
    }
}
