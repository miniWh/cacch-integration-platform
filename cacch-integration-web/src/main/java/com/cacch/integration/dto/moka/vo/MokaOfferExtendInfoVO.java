package com.cacch.integration.dto.moka.vo;

import com.cacch.integration.integration.moka.client.dto.MokaOfferAttachment;
import com.cacch.integration.integration.moka.client.dto.MokaOfferCustomField;
import com.cacch.integration.integration.moka.client.dto.MokaOfferHistory;
import lombok.Data;

import java.util.List;

/**
 * Moka 获取 offer 扩展信息 —— 结果 VO
 *
 * <p>返回 Moka API {@code POST /api-platform/v3/getOfferExtendInfos} 的查询结果。
 * 为简化对接测试，直接复用 integration 层 DTO 类型（{@link MokaOfferCustomField} /
 * {@link MokaOfferAttachment} / {@link MokaOfferHistory}），不做裁剪，
 * 完整透传 Moka 响应核心字段。</p>
 *
 * <p>响应结构：
 * <ul>
 *     <li>{@code success} —— 本服务判定结果（基于 code）</li>
 *     <li>{@code code} —— Moka 原始返回码，透传便于排查</li>
 *     <li>{@code msg} —— Moka 原始消息</li>
 *     <li>{@code data} —— offer 扩展信息列表，元素类型为 {@link OfferExtendInfo}</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaOfferExtendInfoVO {

    /**
     * 本服务判定的调用结果：code 命中成功码（0/200）时为 true
     */
    private Boolean success;

    /**
     * Moka 原始返回码，透传用于排查
     */
    private Integer code;

    /**
     * Moka 原始消息，失败时返回
     */
    private String msg;

    /**
     * offer 扩展信息列表（成功时；失败时为 null）
     */
    private List<OfferExtendInfo> data;

    /**
     * offer 扩展信息 —— VO 层数据结构
     *
     * <p>字段与 integration 层
     * {@link com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoResponse.OfferExtendInfo}
     * 同名同义，由 MapStruct 自动映射；为保持 VO 层独立性，单独声明。</p>
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class OfferExtendInfo {

        /**
         * offer ID
         */
        private Long id;

        /**
         * 租户 ID
         */
        private String orgId;

        /**
         * 薪资
         */
        private String salaryNumber;

        /**
         * 薪资类型：1=时薪，2=日薪，3=月薪，4=年薪
         */
        private Integer salaryType;

        /**
         * 入职日期
         */
        private String checkInDate;

        /**
         * 发送时间（时间戳毫秒）
         */
        private Long sentAt;

        /**
         * offer 状态：pending/accepted/rejected/invalid/expired
         */
        private String status;

        /**
         * 申请 ID
         */
        private Long applicationId;

        /**
         * offer 自定义字段列表
         */
        private List<MokaOfferCustomField> customFields;

        /**
         * 是否向候选人发送生成附件
         */
        private Boolean candidatePdf;

        /**
         * 候选人生成附件加密密码
         */
        private String candidatePdfPassword;

        /**
         * 入职地点 ID
         */
        private Long locationId;

        /**
         * 职位 ID
         */
        private String jobId;

        /**
         * 招聘需求 ID
         */
        private Long hcId;

        /**
         * 生成附件模板 ID
         */
        private Long templateId;

        /**
         * 接受或拒绝 offer 的时间（时间戳毫秒）
         */
        private Long finishedAt;

        /**
         * 职位级别 ID
         */
        private Long jobRankId;

        /**
         * offer 使用的 email 模板 ID
         */
        private Long emailTemplateId;

        /**
         * 直接主管 ID
         */
        private Long employeeId;

        /**
         * 直接主管姓名
         */
        private String employeeName;

        /**
         * 职级 ID
         */
        private Long dutyLevelId;

        /**
         * 职级名称
         */
        private String dutyLevelName;

        /**
         * 入职职位 ID（HCM）
         */
        private Long hcmDutyId;

        /**
         * 入职职位名称（HCM）
         */
        private String hcmDutyName;

        /**
         * 入职部门 ID（HCM）
         */
        private Long hcmDepartmentId;

        /**
         * 入职部门名称（HCM）
         */
        private String hcmDepartmentName;

        /**
         * 创建人 ID
         */
        private Long creatorId;

        /**
         * API 创建人名称
         */
        private String contactName;

        /**
         * API 创建人电话
         */
        private String contactPhone;

        /**
         * API 创建人邮箱
         */
        private String contactEmail;

        /**
         * 候选人附件列表
         */
        private List<MokaOfferAttachment> candidateAttachmentList;

        /**
         * 审批人附件列表
         */
        private List<MokaOfferAttachment> approverAttachmentList;

        /**
         * 创建时间（时间戳毫秒）
         */
        private Long createdAt;

        /**
         * offer 邮件模版内容
         */
        private String content;

        /**
         * 阶段 ID
         */
        private Long stageId;

        /**
         * 职位名称
         */
        private String jobTitle;

        /**
         * 招聘模式：1=社招，2=校招
         */
        private Integer hireMode;

        /**
         * offer 短信模版内容
         */
        private String smsContent;

        /**
         * 发送历史记录列表
         */
        private List<MokaOfferHistory> offerHistoryList;
    }
}
