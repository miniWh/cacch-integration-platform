package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * Moka ATS —— 获取 offer 扩展信息 V3 请求体
 *
 * <p>对接 Moka API {@code POST /api-platform/v3/getOfferExtendInfos}。
 * 字段严格匹配 Moka 官方文档的请求 body 参数表（ApiApplicationIdsDTO）；
 * {@code orgId} 由 Service 层从 {@code MokaProperties} 注入，调用方无需传递。</p>
 *
 * <p>序列化配置 {@code NON_NULL}：null 字段不输出到 JSON，
 * 避免将空值传给 Moka API 导致意外行为。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MokaOfferExtendInfoRequest {

    /**
     * 租户 ID（必填，由 Service 层从配置注入）
     */
    private String orgId;

    /**
     * 申请 ID 列表（必填，单批次最大 100 条）
     */
    private List<Long> applicationIds;

    /**
     * 是否返回 offer 附件（可选，默认 false）
     */
    private Boolean offerAttachment;
}
