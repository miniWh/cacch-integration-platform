package com.cacch.integration.service.moka.api;

import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoResponse;

import java.util.List;

/**
 * Moka offer 扩展信息服务接口
 *
 * <p>职责：封装 Moka ATS offer 域的业务逻辑，作为 {@code MokaOfferController}
 * 的下游服务，对接 Moka API {@code POST /api-platform/v3/getOfferExtendInfos}。</p>
 *
 * <p>当前暴露能力：
 * <ul>
 *     <li>{@link #getOfferExtendInfo(List, Boolean)} —— 按申请 ID 列表查询 offer 扩展信息</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaOfferService {

    /**
     * 获取 offer 扩展信息 —— 调用 Moka {@code POST /api-platform/v3/getOfferExtendInfos}
     *
     * <p>按申请 ID 列表批量查询 offer 扩展信息，含薪资、入职日期、状态、自定义字段、
     * 附件、发送历史等。租户 ID（orgId）由本服务从 {@code moka.org-id} 配置注入，
     * 调用方无需传递。</p>
     *
     * <p>批量限制：单次最多 100 个申请 ID（参考 Moka syncInfo 接口批量限制经验，
     * V3 接口文档未明确声明上限，超出时返回 PARAM_INVALID）。</p>
     *
     * @param applicationIds  申请 ID 列表（必填，非空，单批次 ≤ 100）
     * @param offerAttachment 是否返回 offer 附件（可选，null 时 Moka 默认 false）
     * @return Moka offer 扩展信息响应（含 code/msg/data 列表）
     * @throws com.cacch.integration.common.exception.BizException 参数非法、
     *                                                             API Key / orgId 未配置、
     *                                                             HTTP 调用失败
     *                                                             或 Moka 返回业务失败时抛出
     */
    MokaOfferExtendInfoResponse getOfferExtendInfo(List<Long> applicationIds, Boolean offerAttachment);
}
