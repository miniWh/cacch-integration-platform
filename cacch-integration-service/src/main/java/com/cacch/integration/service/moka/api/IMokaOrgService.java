package com.cacch.integration.service.moka.api;

import com.cacch.integration.integration.moka.client.dto.MokaDeptDeleteRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptDeleteResponse;
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
     * 删除部门 —— 调用 Moka {@code DELETE /api-platform/v1/departments}
     *
     * <p>本项目仅标记删除（不传 target_department_code 合并人员）。
     * 响应为 {success, errorMessage} 结构，批量删除为整体成败语义（success 布尔）。</p>
     *
     * @param request 删除请求体，不可为空；departments 列表与 email（Moka 注册邮箱）均必填
     * @return Moka 删除结果响应
     * @throws com.cacch.integration.common.exception.BizException Moka API Key 未配置、
     *                                                             HTTP 调用失败或 Moka 返回 success=false 时抛出
     */
    MokaDeptDeleteResponse deleteDepartments(MokaDeptDeleteRequest request);
}
