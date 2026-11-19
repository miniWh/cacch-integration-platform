package com.cacch.integration.service.moka.api;

import com.cacch.integration.integration.moka.client.dto.MokaApplicationListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaEhrApplicationsV2Request;
import com.cacch.integration.integration.moka.client.dto.MokaEhrApplicationsV2Response;

/**
 * Moka 候选人服务接口
 *
 * <p>职责：封装 Moka ATS 候选人域（Candidates）的业务逻辑，作为 MokaCandidatesController 的下游服务。</p>
 *
 * <p>当前暴露能力：
 * <ul>
 *     <li>{@link #listApplications(MokaApplicationListRequest)} — 根据条件查询申请信息（v3）</li>
 *     <li>{@link #listEhrApplicationsV2(MokaEhrApplicationsV2Request)} — 获取候选人信息 v2（EHR）</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaCandidatesService {

    /**
     * 根据条件查询申请信息（分页）—— 调用 Moka {@code POST /api-platform/v3/applications/list_by_condition}
     *
     * <p>支持按申请 id、候选人 id、职位 id、流程、阶段、招聘模式、归档状态、时间范围等条件查询。
     * 分页通过 {@code limit}（默认 20，最大 20）和 {@code next}（游标）实现。</p>
     *
     * <p>注意：成功码为 200（与部门接口的 code=0 不同）。</p>
     *
     * @param request 查询条件（所有字段均可选，null 时自动兜底空对象表示无条件查询全部）
     * @return 申请分页响应（含 applicationList + next 分页游标）
     * @throws com.cacch.integration.common.exception.BizException Moka API Key 未配置、
     *                                                             HTTP 调用失败或 Moka 返回业务失败时抛出
     */
    MokaApplicationListResponse listApplications(MokaApplicationListRequest request);

    /**
     * 获取候选人信息 v2 —— 调用 Moka {@code POST /api-platform/v2/data/ehrApplications}
     *
     * <p>返回含 {@code basicInfo}、{@code jobInfo} 等完整 EHR 结构；分页通过 {@code limit} 与顶层 {@code next} 实现。</p>
     *
     * @param request 查询条件（所有字段均可选，null 时自动兜底空对象）
     * @return Moka 原始响应（含 code、msg、next、data 数组）
     * @throws com.cacch.integration.common.exception.BizException Moka API Key 未配置、
     *                                                             HTTP 调用失败或 Moka 返回业务失败时抛出
     */
    MokaEhrApplicationsV2Response listEhrApplicationsV2(MokaEhrApplicationsV2Request request);
}
