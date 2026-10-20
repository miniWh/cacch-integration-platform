package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * Moka ATS —— 标记候选人已入职请求体
 *
 * <p>对接 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/hired}。
 * 字段严格匹配 Moka 官方文档的请求 body 参数表。</p>
 *
 * <p>业务规则（由 Moka 侧处理，本类不做校验）：
 * <ul>
 *     <li>{@code hcId} 仅在 {@code isUseOfferHcId=1} 时不传参会被自动填入当前申请下 offer
 *         所关联的招聘需求 id；若 offer 关联的招聘需求 id 为空，则该参数为空。</li>
 *     <li><strong>{@code hcId} 传 null 视为非法（不允许 {@code hcId=""} 或 {@code hcId=null}），
 *         不需要该字段时直接不传</strong>；本类通过 {@code NON_NULL} 序列化配置保证 null 字段
 *         不输出到 JSON，天然满足该约束。</li>
 *     <li>{@code talentPoolIds} 为空或未包含任一入职人才库 id 时，候选人被放入「系统入职人才库」；
 *         含「入职人才库」id 时放入指定入职人才库；含「非入职人才库」id 时不会放入该人才库。</li>
 * </ul></p>
 *
 * <p>序列化配置 {@code NON_NULL}：null 字段不输出到 JSON，由 Moka 侧取默认值，
 * 避免将空值显式传给 Moka API 导致意外行为。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MokaApplicationHiredRequest {

    /**
     * 招聘需求 id（可选）；为 null 时不允许传给 Moka，须直接不传该字段
     */
    private Integer hcId;

    /**
     * 实际入职时间（必填）
     */
    private String hiredAt;

    /**
     * 试用期（必填）：0=无试用期，1-6 对应几个月试用期
     */
    private Integer probation;

    /**
     * 入职时是否使用 offer 关联的招聘需求（可选）：传 1 表示启用，
     * 启用后 {@link #hcId} 不传参会自动填入 offer 所关联的招聘需求 id；不传则不执行该逻辑
     */
    private Integer isUseOfferHcId;

    /**
     * 人才库 id 列表（可选）
     */
    private List<Integer> talentPoolIds;
}
