package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Moka ATS —— 获取 offer 扩展信息 V3 响应
 *
 * <p>对接 Moka API {@code POST /api-platform/v3/getOfferExtendInfos}。
 * 响应结构为 Moka 通用单层结构 { code, msg, data }，业务数据 {@code data}
 * 为 offer 扩展信息列表，元素结构见 {@link OfferExtendInfo}。</p>
 *
 * <p><strong>注意</strong>：Moka 实际返回字段名 {@code checkinDate}（全小写 n），
 * 不符合 Java 驼峰命名规范（应为 {@code checkInDate}）。为保留 Java 命名规范，
 * 通过 {@code @JsonProperty("checkinDate")} 注解映射 JSON 字段名到 Java 字段名。</p>
 *
 * <p>字段覆盖 Moka 文档所列全部字段（薪资、入职日期、状态、自定义字段、
 * 附件、发送历史、HCM 职位/部门、主管、审批人等）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MokaOfferExtendInfoResponse
        extends MokaBaseResponse<List<MokaOfferExtendInfoResponse.OfferExtendInfo>> {

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
         * <p><strong>重要</strong>：Moka 实际返回的 JSON 字段名为 {@code checkinDate}
         * （全小写 n，不符合驼峰规范），通过 {@code @JsonProperty("checkinDate")}
         * 映射到 Java 字段 {@code checkInDate}，保留 Java 命名规范。</p>
         *
         * <p>实际取值示例：{@code "2026-11-01"}（yyyy-MM-dd 格式字符串）。</p>
         */
        @JsonProperty("checkinDate")
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
