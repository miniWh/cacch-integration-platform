package com.cacch.integration.dto.moka.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/**
 * Moka 标记候选人已入职 —— web 层请求体
 *
 * <p>对接测试接口 {@code PUT /api/v1/moka/applications/{applicationId}/hired}，
 * 经 MapStruct 转换为 integration 层 {@code MokaApplicationHiredRequest} 后调用 Moka API。</p>
 *
 * <p>校验规则：
 * <ul>
 *     <li>{@code hiredAt}（实际入职时间）必填</li>
 *     <li>{@code probation}（试用期）必填，取值 0-6</li>
 *     <li>{@code hcId} 可选，若传须为正整数；不传时不发送给 Moka</li>
 *     <li>{@code isUseOfferHcId} 可选，取值 0/1</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaApplicationHiredRequest {

    /**
     * 招聘需求 id（可选，若传须为正整数；为 null 时不发送给 Moka）
     */
    @Positive(message = "招聘需求 id hcId 必须为正整数")
    private Integer hcId;

    /**
     * 实际入职时间（必填）
     */
    @NotBlank(message = "实际入职时间 hiredAt 不可为空")
    private String hiredAt;

    /**
     * 试用期（必填）：0=无试用期，1-6 对应几个月试用期
     */
    @NotNull(message = "试用期 probation 不可为空")
    @Min(value = 0, message = "试用期 probation 取值 0-6")
    @Max(value = 6, message = "试用期 probation 取值 0-6")
    private Integer probation;

    /**
     * 入职时是否使用 offer 关联的招聘需求（可选）：传 1 表示启用，不传则不执行该逻辑
     */
    @Min(value = 0, message = "isUseOfferHcId 取值 0 或 1")
    @Max(value = 1, message = "isUseOfferHcId 取值 0 或 1")
    private Integer isUseOfferHcId;

    /**
     * 人才库 id 列表（可选，需先通过 Moka 人才库列表接口查询）
     */
    private List<Integer> talentPoolIds;
}
