package com.cacch.integration.dto.moka.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Moka 获取 offer 扩展信息 —— web 层请求体
 *
 * <p>对接测试接口 {@code POST /api/v1/moka/offers/extend-infos}，
 * 经 MapStruct 转换后调用 Moka API {@code POST /api-platform/v3/getOfferExtendInfos}。
 * 租户 ID（orgId）由 Service 层从配置注入，调用方无需传递。</p>
 *
 * <p>校验规则：
 * <ul>
 *     <li>{@code applicationIds} 必填，非空集合，单批次 ≤ 100 条</li>
 *     <li>{@code offerAttachment} 可选，不传时 Moka 默认 false</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaOfferExtendInfoRequestVO {

    /**
     * 申请 ID 列表（必填，单批次最多 100 条）
     */
    @NotEmpty(message = "applicationIds 不可为空")
    @Size(max = 100, message = "applicationIds 单批次最多 100 条")
    private List<Long> applicationIds;

    /**
     * 是否返回 offer 附件（可选，默认 false）
     */
    private Boolean offerAttachment;
}
