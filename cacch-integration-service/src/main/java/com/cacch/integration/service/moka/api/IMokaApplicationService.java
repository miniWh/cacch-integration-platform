package com.cacch.integration.service.moka.api;

import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignResponse;

/**
 * Moka 入离职服务接口
 *
 * <p>职责：封装 Moka ATS 申请域（Application）的业务逻辑，作为MokaApplicationController}的下游服务。</p>
 *
 * <p>当前暴露能力：
 * <ul>
 *     <li>{@link #resignApplication(Integer, MokaApplicationResignRequest)} — 标记候选人已离职</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaApplicationService {

    /**
     * 标记候选人已离职 —— 调用 Moka {@code PUT /api-platform/v1/applications/{applicationId}/resign}
     *
     * <p>将指定申请归档为已离职。请求体中 {@code detail}（离职原因）必填；
     * {@code isResignWriteJob=1} 时 {@code resignJobName} 必填（本服务不校验该条件依赖，
     * 由调用方保证；Moka 侧亦会校验）。</p>
     *
     * <p>注意：本方法为写操作，调用成功后 Moka 侧申请将归档为已离职，不可自动回滚，
     * 测试环境请使用测试申请 id 验证。</p>
     *
     * @param applicationId 候选人申请 id（必填，大于 0），来自 Moka 申请查询接口
     * @param request       离职请求体（必填，detail 必填）
     * @return Moka 离职操作响应（code + errorMessage）
     * @throws com.cacch.integration.common.exception.BizException 参数非法、
     *                                                             Moka API Key 未配置、HTTP 调用失败
     *                                                             或 Moka 返回业务失败时抛出
     */
    MokaApplicationResignResponse resignApplication(Integer applicationId, MokaApplicationResignRequest request);
}
