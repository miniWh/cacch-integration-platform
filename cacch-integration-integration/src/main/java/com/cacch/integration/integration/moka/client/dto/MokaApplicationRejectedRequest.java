package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Moka ATS —— 标记候选人未入职请求体
 *
 * <p>对接 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/rejected}。
 * 字段严格匹配 Moka 官方文档的请求 body 参数表，全部字段均可选。</p>
 *
 * <p>业务规则（由 Moka 侧处理，本类不做校验）：
 * <ul>
 *     <li>{@code archiveReasonId} 不传时，Moka 默认使用内置的「福利待遇不满足」归档原因</li>
 *     <li>{@code talentPoolId} 不传时，Moka 默认使用内置的「系统公共人才库」；
 *         如需指定其他人才库，须先通过「查询人才库列表接口」获取有效 id</li>
 *     <li>调用方须确保申请处于「待入职」阶段且不在人才库中，否则 Moka 返回失败</li>
 * </ul></p>
 *
 * <p>序列化配置 {@code NON_NULL}：null 字段不输出到 JSON，由 Moka 侧取默认值，
 * 避免将空值显式传给 Moka API 导致意外行为。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MokaApplicationRejectedRequest {

    /**
     * 归档原因 id（可选，不传默认为内置的「福利待遇不满足」）
     */
    private Integer archiveReasonId;

    /**
     * 人才库 id（可选，不传默认为内置的「系统公共人才库」）
     */
    private String talentPoolId;

    /**
     * 归档详细原因（可选，可以为空）
     */
    private String detail;
}
