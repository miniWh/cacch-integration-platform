package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * Moka —— 获取候选人信息 v2（EHR 申请）请求体
 *
 * <p>POST {@code /api-platform/v2/data/ehrApplications}。
 * 所有字段均为可选；{@code limit} 默认 20、最大 20；分页可仅传 {@code next}。</p>
 *
 * <p>序列化配置 {@code NON_NULL}：null 字段不输出到 JSON。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MokaEhrApplicationsV2Request {

    /**
     * 候选人邮箱
     */
    private String email;

    /**
     * 申请 id 列表
     */
    private List<Long> applicationIds;

    /**
     * 邀请更新状态：0=未邀请，1=未更新，2=已更新
     */
    private Integer invitationUpdateStatus;

    /**
     * 分页每页条数，默认 20，最大 20
     */
    private Integer limit;

    /**
     * 分页游标；可仅传此字段继续翻页
     */
    private String next;

    /**
     * 候选人手机号
     */
    private String phone;

    /**
     * 为 true 时返回 offer 附件下载链接（约 1 小时有效）
     */
    private Boolean offerAttachment;

    /**
     * 为 true 时返回候选人自定义样式登记表
     */
    private Boolean applicationForm;

    /**
     * 招聘流程 id 列表
     */
    private List<Long> pipelineIds;

    /**
     * 阶段 id 列表
     */
    private List<Long> stageIds;

    /**
     * 更新开始时间（北京时间，格式 {@code YYYY-MM-DDTHH:mm:ss.sssZ}）
     */
    private String updateAtStartTime;

    /**
     * 更新结束时间（北京时间，格式 {@code YYYY-MM-DDTHH:mm:ss.sssZ}）
     */
    private String updateAtEndTime;

    /**
     * 是否返回附件；不传默认 true
     */
    private Boolean needAttachment;

    /**
     * 申请是否归档：true=已归档，false=未归档
     */
    private Boolean archived;

    /**
     * 招聘模式：1=社招，2=校招
     */
    private Integer hireMode;

    /**
     * 数据创建开始时间（毫秒时间戳，申请首次入库时间）
     */
    private Long applicationCreateStartTime;

    /**
     * 数据创建结束时间（毫秒时间戳）
     */
    private Long applicationCreateEndTime;

    /**
     * 申请展示日期开始时间（毫秒时间戳）
     */
    private Long applicationAppliedAtStartTime;

    /**
     * 申请展示日期结束时间（毫秒时间戳）
     */
    private Long applicationAppliedAtEndTime;
}