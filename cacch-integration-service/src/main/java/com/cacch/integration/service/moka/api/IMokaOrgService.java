package com.cacch.integration.service.moka.api;

import com.cacch.integration.integration.moka.client.dto.MokaApplicationListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDeptListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncResponse;

/**
 * Moka 组织架构服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaOrgService {

    /**
     * 组织架构全量同步 —— 调用 Moka {@code PUT /api-platform/v2/departments}
     *
     * <p>同步以 departmentCode 为主键，Moka 侧自动执行新增 / 更新 / 标记删除。</p>
     *
     * @param request 同步请求体，不可为空；departments 列表必填
     * @return Moka 同步结果响应（含 new / update / delete 数量）
     * @throws com.cacch.integration.common.exception.BizException Moka API Key 未配置、
     *                                                             HTTP 调用失败或 Moka 返回业务失败时抛出
     */
    MokaDeptSyncResponse syncDepartmentsFull(MokaDeptSyncRequest request);

    /**
     * 获取全量组织架构 —— 调用 Moka {@code GET /api-platform/v1/departments}
     *
     * <p>返回 Moka 侧全量部门列表。支持可选 {@code updateTimeStart} 增量查询参数
     * （格式 {@code yyyy-MM-dd HH:mm:ss}），为空时返回全量数据。</p>
     *
     * @param updateTimeStart 增量查询起始时间（可选，格式 yyyy-MM-dd HH:mm:ss）；
     *                        为 null 或空白时返回全量部门
     * @return Moka 全量组织架构响应（data 为部门列表）
     * @throws com.cacch.integration.common.exception.BizException Moka API Key 未配置、
     *                                                             HTTP 调用失败或 Moka 返回业务失败时抛出
     */
    MokaDeptListResponse getDepartments(String updateTimeStart);

    /**
     * 根据条件查询申请信息（分页）—— 调用 Moka {@code POST /api-platform/v3/applications/list_by_condition}
     *
     * <p>支持按申请 id、候选人 id、职位 id、流程、阶段、招聘模式、归档状态、时间范围等条件查询。
     * 分页通过 {@code limit}（默认 20，最大 20）和 {@code next}（游标）实现。</p>
     *
     * @param request 查询条件（所有字段均可选，空对象表示返回全部）
     * @return 申请分页响应（含 applicationList + next 分页游标）
     * @throws com.cacch.integration.common.exception.BizException Moka API Key 未配置、
     *                                                             HTTP 调用失败或 Moka 返回业务失败时抛出
     */
    MokaApplicationListResponse listApplications(MokaApplicationListRequest request);

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
