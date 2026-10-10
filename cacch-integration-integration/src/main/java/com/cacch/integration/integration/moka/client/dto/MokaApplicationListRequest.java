package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * Moka ATS —— 根据条件查询申请信息（分页）请求体
 *
 * <p>POST {@code /api-platform/v3/applications/list_by_condition}。
 * 所有字段均为可选；不传时返回全部申请。
 * 时间戳字段单位为**毫秒**（与 Moka API 一致），最大时间窗口 3 个月。</p>
 *
 * <p>序列化配置 {@code NON_NULL}：null 字段不输出到 JSON，
 * 避免将空值传给 Moka API 导致意外的过滤效果。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MokaApplicationListRequest {

    /**
     * 申请 ids，最大长度 20
     */
    private List<Long> applicationIds;

    /**
     * 候选人 ids，最大长度 20
     */
    private List<Long> candidateIds;

    /**
     * 职位 id
     */
    private String jobId;

    /**
     * 流程 ids
     */
    private List<Long> pipelineIds;

    /**
     * 阶段 ids
     */
    private List<Long> stageIds;

    /**
     * 招聘模式：1=社招，2=校招；传其他值视为没传
     */
    private Integer hireMode;

    /**
     * 申请是否归档：true=已归档，false=未归档
     */
    private Boolean archived;

    /**
     * 邀请更新状态：0=未邀请，1=未更新，2=已更新
     */
    private Integer invitationUpdateStatus;

    /**
     * 更新开始时间（时间戳毫秒，最大时间范围 3 个月）
     */
    private Long updateAtStartTime;

    /**
     * 更新结束时间（时间戳毫秒，最大时间范围 3 个月）
     */
    private Long updateAtEndTime;

    /**
     * 数据创建开始时间（时间戳毫秒，指申请首次被保存至数据库的时间）
     */
    private Long applicationCreateStartTime;

    /**
     * 数据创建结束时间（时间戳毫秒）
     */
    private Long applicationCreateEndTime;

    /**
     * 申请创建开始时间（时间戳毫秒，指系统展示的申请日期）
     */
    private Long applicationAppliedAtStartTime;

    /**
     * 申请创建结束时间（时间戳毫秒）
     */
    private Long applicationAppliedAtEndTime;

    /**
     * 分页每页条数，默认 20，最大 20
     */
    private Integer limit;

    /**
     * 传入后取消 3 个月时间窗口限制，仅返回该猎头顾问名下申请
     */
    private Long headhunterId;

    /**
     * 传入后取消 3 个月时间窗口限制，仅返回该猎头合同名下申请
     */
    private Long headhunterContractId;

    /**
     * 传入后取消 3 个月时间窗口限制，仅返回该申请来源名下申请
     */
    private Long sourceId;

    /**
     * 分页游标，只传这一个就可以（由上次响应 data.next 返回）
     */
    private String next;
}
