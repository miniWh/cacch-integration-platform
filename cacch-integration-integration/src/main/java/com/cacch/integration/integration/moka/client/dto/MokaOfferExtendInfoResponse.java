package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Moka ATS —— 获取 offer 扩展信息 V3 响应
 *
 * <p>对接 Moka API {@code POST /api-platform/v3/getOfferExtendInfos}。
 * 外层响应为 Moka 通用结构 { code, msg, data }，通用字段继承自
 * {@link MokaBaseResponse}；Moka 文档另声明 {@code success}（Boolean）字段，
 * 但成功判定仍以 {@code code} 为准（沿用既有 Moka 对接约定，{@code success}
 * 字段不纳入基类，避免影响其他 Moka 接口）。</p>
 *
 * <p>{@code data} 为 offer 扩展信息列表，元素结构见 {@link OfferExtendInfo}，
 * 字段覆盖 Moka 文档 5 张图片所列全部字段（薪资、入职日期、状态、自定义字段、
 * 附件、发送历史、HCM 职位/部门、主管、审批人等）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MokaOfferExtendInfoResponse
        extends MokaBaseResponse<List<MokaOfferExtendInfoResponse.OfferExtendInfo>> {

    /**
     * Moka 文档声明的 success 字段（Boolean）—— 仅接收不参与判定
     *
     * <p>成功判定仍由 {@link MokaBaseResponse#isSuccess()} 兼容 code=0 / code=200 完成。</p>
     */
    @JsonProperty("success")
    private Boolean successField;

    /**
     * offer 扩展信息 —— 列表元素
     *
     * <p>字段覆盖 Moka 文档所列全部字段，按职责分组注释：
     * <ul>
     *     <li>基础信息：id / orgId / applicationId / status</li>
     *     <li>薪资信息：salaryNumber / salaryType</li>
     *     <li>时间信息：checkInDate / sentAt / finishedAt / createdAt</li>
     *     <li>自定义字段：customFields</li>
     *     <li>附件与内容：candidatePdf / candidatePdfPassword / content / smsContent /
     *         candidateAttachmentList / approverAttachmentList</li>
     *     <li>职位与部门：jobId / jobTitle / jobRankId / hcId / locationId /
     *         templateId / emailTemplateId / stageId / hireMode</li>
     *     <li>主管与职级：employeeId / employeeName / dutyLevelId / dutyLevelName</li>
     *     <li>HCM 字段：hcmDutyId / hcmDutyName / hcmDepartmentId / hcmDepartmentName</li>
     *     <li>创建人联系方式：creatorId / contactName / contactPhone / contactEmail</li>
     *     <li>发送历史：offerHistoryList</li>
     * </ul></p>
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
         * 薪资（String 承接，避免精度丢失）
         */
        private String salaryNumber;

        /**
         * 薪资类型：1=时薪，2=日薪，3=月薪，4=年薪
         */
        private Integer salaryType;

        /**
         * 入职日期（字符串格式）
         *
         * <p><strong>注意</strong>：Moka 实际返回的入职日期不在此字段（始终为 null），
         * 而是在 {@link #offerAttachmentList} 的首个元素 {@code checkInDate} 中。
         * 本字段保留仅为兼容文档声明，实际取值请用
         * {@code offerAttachmentList[0].checkInDate}。</p>
         */
        private String checkInDate;

        /**
         * 发送时间（时间戳毫秒）
         */
        private Long sentAt;

        /**
         * offer 状态：pending / accepted / rejected / invalid / expired
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
         * 职位 ID（String 类型，与其他 ID 的 Long 类型不同）
         */
        private String jobId;

        /**
         * 招聘需求 ID（HC = Headcount）
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
         * 入职职位 ID（HCM 系统）
         */
        private Long hcmDutyId;

        /**
         * 入职职位名称（HCM 系统）
         */
        private String hcmDutyName;

        /**
         * 入职部门 ID（HCM 系统）
         */
        private Long hcmDepartmentId;

        /**
         * 入职部门名称（HCM 系统）
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
         * offer 附件列表（Moka 实际返回的 offer 附件，含入职日期等关键字段）
         *
         * <p><strong>重要</strong>：{@link #checkInDate} 实际位于本列表首个元素中，
         * 而非 OfferExtendInfo 顶层。此字段由 raw 日志确认，Moka 文档未明确声明。</p>
         */
        private List<MokaOfferAttachmentDetail> offerAttachmentList;

        /**
         * 创建时间（时间戳毫秒）
         */
        private Long createdAt;

        /**
         * offer 邮件模版内容
         */
        private String content;

        /**
         * 阶段 ID（招聘流程阶段）
         */
        private Long stageId;

        /**
         * 职位名称（jobTitle）
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
