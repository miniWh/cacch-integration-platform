package com.cacch.integration.service.moka.api;

import com.cacch.integration.integration.moka.client.dto.MokaJobListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaJobListResponse;

/**
 * Moka 职位服务接口
 *
 * <p>职责：封装 Moka ATS 职位域（Jobs）的业务逻辑，作为 MokaJobController 的下游服务。</p>
 *
 * <p>当前暴露能力：
 * <ul>
 *     <li>{@link #getJobs(MokaJobListRequest)} — 查询职位信息</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaJobService {

    /**
     * 查询职位信息 —— 调用 Moka {@code POST /api-platform/v1/jobs/getJobs}
     *
     * <p>按招聘模式（必填）及地点、职能、官网、职位性质、状态、部门、门店、
     * 更新时间、自定义字段等可选条件查询职位，出参全量透传。</p>
     *
     * @param request 查询条件；hireMode 必填（social=社招 / campus=校招），其余字段可选
     * @return 职位查询响应（含 total 职位总数与 jobs 职位列表）
     * @throws com.cacch.integration.common.exception.BizException Moka API Key 未配置、
     *                                                             HTTP 调用失败或 Moka 返回业务失败时抛出
     */
    MokaJobListResponse getJobs(MokaJobListRequest request);
}
